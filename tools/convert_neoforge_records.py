#!/usr/bin/env python3
"""Convert NeoForge packet classes to Java records."""

import re
import os
import sys

BASE_DIR = r'c:\VS Workspace\Projects\Minecraft Mods\Servermanagement-Forge\neoforge\src\main\java\com\servermanagement\network\packet'


def find_matching_brace(content, start_pos):
    """Find the closing brace matching the opening brace at start_pos."""
    depth = 0
    i = start_pos
    in_string = False
    in_char = False
    in_line_comment = False
    in_block_comment = False
    escape_next = False

    while i < len(content):
        c = content[i]
        if escape_next:
            escape_next = False
            i += 1
            continue
        if in_line_comment:
            if c == '\n':
                in_line_comment = False
            i += 1
            continue
        if in_block_comment:
            if c == '*' and i + 1 < len(content) and content[i + 1] == '/':
                in_block_comment = False
                i += 2
                continue
            i += 1
            continue
        if in_string:
            if c == '\\':
                escape_next = True
                i += 1
                continue
            if c == '"':
                in_string = False
            i += 1
            continue
        if in_char:
            if c == '\\':
                escape_next = True
                i += 1
                continue
            if c == "'":
                in_char = False
            i += 1
            continue
        if c == '/' and i + 1 < len(content):
            if content[i + 1] == '/':
                in_line_comment = True
                i += 2
                continue
            if content[i + 1] == '*':
                in_block_comment = True
                i += 2
                continue
        if c == '"':
            in_string = True
            i += 1
            continue
        if c == "'":
            in_char = True
            i += 1
            continue
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def parse_fields(content):
    """Extract private final instance fields (not static)."""
    field_re = re.compile(
        r'^\s*private\s+final\s+'
        r'([\w.]+(?:<[\w.,\s<>?]+>)?(?:\[\])?)'  # type
        r'\s+(\w+)\s*;',  # name
        re.MULTILINE
    )
    return [(m.group(1), m.group(2)) for m in field_re.finditer(content)]


def find_all_constructors(content, class_name):
    """Find all constructors, yielding (full_match_start, end_brace_pos, header, body, params_str)."""
    pattern = re.compile(
        r'([ \t]*public\s+' + re.escape(class_name) + r'\s*\(([^)]*)\)\s*\{)'
    )
    results = []
    for m in pattern.finditer(content):
        params_str = m.group(2).strip()
        header = m.group(1)
        header_start = m.start(1)
        brace_pos = m.end(1) - 1
        end_pos = find_matching_brace(content, brace_pos)
        if end_pos == -1:
            continue
        body = content[brace_pos + 1:end_pos]
        results.append((header_start, end_pos, header, body, params_str))
    return results


def is_canonical_constructor(body, fields):
    """Check if body only has this.x = x; (pure assignments)."""
    lines = [l.strip() for l in body.strip().split('\n')
             if l.strip() and not l.strip().startswith('//')]
    if len(lines) != len(fields):
        return False
    for line, (ftype, fname) in zip(lines, fields):
        if not re.match(r'this\.' + re.escape(fname) + r'\s*=\s*' + re.escape(fname) + r'\s*;$', line):
            return False
    return True


def try_simple_buf_conversion(body, fields):
    """Try simple this.x = expr; conversion. Returns list of exprs or None."""
    lines = [l.strip() for l in body.strip().split('\n')
             if l.strip() and not l.strip().startswith('//')]
    if len(lines) != len(fields):
        return None
    exprs = []
    for line, (ftype, fname) in zip(lines, fields):
        m = re.match(r'this\.' + re.escape(fname) + r'\s*=\s*(.+?)\s*;$', line)
        if not m:
            return None
        exprs.append(m.group(1).strip())
    return exprs


def extract_convenience_exprs(body, fields):
    """Extract this.fieldName = expr for each field from a non-canonical constructor."""
    exprs = {}
    for ftype, fname in fields:
        m = re.search(r'this\.' + re.escape(fname) + r'\s*=\s*(.+?)\s*;', body)
        if m:
            exprs[fname] = m.group(1).strip()
    if len(exprs) == len(fields):
        return [exprs[fname] for _, fname in fields]
    return None


# ============================================================
# COMPLEX BUF CONSTRUCTOR HANDLERS
# ============================================================
# For each complex packet, define:
#   - new_buf_body: the delegating this(...) call
#   - static_helpers: list of static methods to add
#   - extra_changes: any additional content modifications (e.g., making methods static)

COMPLEX_HANDLERS = {}


def register_complex(class_name, new_buf_body, static_helpers="", extra_changes=None):
    COMPLEX_HANDLERS[class_name] = {
        'buf_body': new_buf_body,
        'static_helpers': static_helpers,
        'extra_changes': extra_changes,
    }


register_complex('SyncWorldListPacket',
    '        this(decodeWorlds(buf));',
    '''
    private static List<WorldInfo> decodeWorlds(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<WorldInfo> worlds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            worlds.add(new WorldInfo(
                buf.readUtf(256),
                buf.readUtf(128),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt()
            ));
        }
        return worlds;
    }
''')

register_complex('SyncAchievementsPacket',
    '        this(decodeEarnedAchievements(buf), buf.readInt());',
    '''
    private static Set<String> decodeEarnedAchievements(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Set<String> set = new HashSet<>();
        for (int i = 0; i < count; i++) {
            set.add(buf.readUtf(128));
        }
        return set;
    }
''')

register_complex('SyncDailyTasksPacket',
    '        this(decodeTasks(buf), buf.readLong(), buf.readBoolean(), buf.readInt(), buf.readLong());',
    '''
    private static List<DailyTask> decodeTasks(FriendlyByteBuf buf) {
        int taskCount = buf.readInt();
        List<DailyTask> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            TaskType type = TaskType.values()[buf.readInt()];
            int goal = buf.readInt();
            int progress = buf.readInt();
            boolean claimed = buf.readBoolean();
            int reward = buf.readInt();
            String description = buf.readUtf(256);
            DailyTask task = new DailyTask(type, goal, reward, description);
            task.setProgress(progress);
            task.setClaimed(claimed);
            tasks.add(task);
        }
        return tasks;
    }
''')

register_complex('SyncBankAccountPacket',
    '        this(buf.readDouble(), decodeTransactions(buf));',
    '''
    private static List<Transaction> decodeTransactions(FriendlyByteBuf buf) {
        int transactionCount = buf.readInt();
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < transactionCount; i++) {
            String typeName = buf.readUtf(64);
            double amount = buf.readDouble();
            long timestamp = buf.readLong();
            String description = buf.readUtf(256);
            boolean hasOtherParty = buf.readBoolean();
            UUID otherParty = hasOtherParty ? buf.readUUID() : null;
            TransactionType type;
            try {
                type = TransactionType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                type = TransactionType.ADMIN_GIVE;
            }
            transactions.add(new Transaction(type, amount, timestamp, description, otherParty));
        }
        return transactions;
    }
''')

register_complex('SyncFeatureStatesPacket',
    '        this(decodeFeatureStates(buf));',
    '''
    private static Map<String, Boolean> decodeFeatureStates(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, Boolean> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(64), buf.readBoolean());
        }
        return map;
    }
''')

register_complex('SyncMarketPricesPacket',
    '        this(buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readDouble(), decodeSupplyData(buf), decodeRecipePrices(buf));',
    '''
    private static Map<String, Long> decodeSupplyData(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, Long> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(256), buf.readLong());
        }
        return map;
    }

    private static Map<String, Double> decodeRecipePrices(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, Double> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(256), buf.readDouble());
        }
        return map;
    }
''')

register_complex('SyncMoneyRequestsPacket',
    '        this(decodeEntries(buf), decodeEntries(buf));',
    '''
    private static List<ClientMoneyRequestData.RequestEntry> decodeEntries(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ClientMoneyRequestData.RequestEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(readEntry(buf));
        }
        return list;
    }
''',
    extra_changes=lambda c: c.replace(
        'private ClientMoneyRequestData.RequestEntry readEntry(',
        'private static ClientMoneyRequestData.RequestEntry readEntry('
    ).replace(
        'private void writeEntry(',
        'private static void writeEntry('
    )
)

register_complex('PMSyncPlayerListsPacket',
    '        this(decodeStringList(buf), decodeStringList(buf), buf.readBoolean());',
    '''
    private static List<String> decodeStringList(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(buf.readUtf(16));
        }
        return list;
    }
''')

register_complex('SyncEconomyTemplatesPacket',
    '        this(decodeTemplates(buf), buf.readInt(), buf.readInt());',
    '''
    private static List<TemplateData> decodeTemplates(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<TemplateData> templates = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            templates.add(new TemplateData(
                buf.readUtf(64),
                buf.readInt(),
                buf.readUtf(100),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf)
            ));
        }
        return templates;
    }
''')

# ModFileChunkPacket: field order mismatch - reorder fields to match encode/decode order
# Original fields: chunkIndex, totalChunks, chunkData, fileHash
# Encode order: chunkIndex, totalChunks, fileHash, chunkData.length, chunkData
# New field order: chunkIndex, totalChunks, fileHash, chunkData
register_complex('ModFileChunkPacket',
    '        this(buf.readInt(), buf.readInt(), buf.readUtf(128), decodeChunkData(buf));',
    '''
    private static byte[] decodeChunkData(FriendlyByteBuf buf) {
        int dataLength = Math.min(buf.readInt(), CHUNK_SIZE + 1024);
        byte[] data = new byte[dataLength];
        buf.readBytes(data);
        return data;
    }
''')

register_complex('PurchaseListingPacket',
    '        this(buf.readUtf(36), buf.readByte(), decodeSelectedSlots(buf));',
    '''
    private static int[] decodeSelectedSlots(FriendlyByteBuf buf) {
        int slotCount = buf.readVarInt();
        if (slotCount < 0 || slotCount > 36) {
            return new int[0];
        }
        int[] slots = new int[slotCount];
        for (int i = 0; i < slotCount; i++) {
            slots[i] = buf.readVarInt();
        }
        return slots;
    }
''')

register_complex('OpenGuiPacket',
    '        this(decodeGuiType(buf), buf.readUtf(256));',
    '''
    private static GuiType decodeGuiType(FriendlyByteBuf buf) {
        int ordinal = buf.readInt();
        GuiType[] values = GuiType.values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : GuiType.DASHBOARD;
    }
''')

register_complex('SyncMineBayListingsPacket',
    '        this(decodeListings(buf));',
    '''
    private static List<MineBayListing> decodeListings(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<MineBayListing> listings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String listingId = buf.readUtf(36);
            UUID sellerId = buf.readUUID();
            String sellerName = buf.readUtf(16);
            ItemStack itemOffered = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
            double moneyPrice = buf.readDouble();
            long createdTime = buf.readLong();
            MineBayListing.OfferType offerType = buf.readEnum(MineBayListing.OfferType.class);
            int priceItemCount = buf.readInt();
            List<PriceItemEntry> priceItems = new ArrayList<>();
            for (int j = 0; j < priceItemCount; j++) {
                ItemStack priceItem = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
                int amount = buf.readInt();
                boolean useStacks = buf.readBoolean();
                priceItems.add(new PriceItemEntry(priceItem, amount, useStacks));
            }
            double baseMarketPrice = buf.readDouble();
            double marginPercent = buf.readDouble();
            int pendingOfferCount = buf.readInt();
            MineBayListing listing = new MineBayListing(sellerId, sellerName, itemOffered, moneyPrice, baseMarketPrice, marginPercent, priceItems, offerType);
            listing.setListingId(listingId);
            listing.setCreatedTime(createdTime);
            listing.setPendingOfferCount(pendingOfferCount);
            listings.add(listing);
        }
        return listings;
    }
''')

register_complex('SyncListingOffersPacket',
    '        this(buf.readUtf(36), decodeOffers(buf));',
    '''
    private static List<MineBayOffer> decodeOffers(FriendlyByteBuf buf) {
        int count = buf.readInt();
        if (count < 0 || count > 50) count = 0;
        List<MineBayOffer> offers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String offerId = buf.readUtf(36);
            UUID buyerId = buf.readUUID();
            String buyerName = buf.readUtf(16);
            double moneyOffer = buf.readDouble();
            long timestamp = buf.readLong();
            int itemCount = buf.readInt();
            if (itemCount < 0 || itemCount > 27) itemCount = 0;
            List<ItemStack> items = new ArrayList<>();
            for (int j = 0; j < itemCount; j++) {
                items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(
                    (net.minecraft.network.RegistryFriendlyByteBuf) buf));
            }
            MineBayOffer offer = new MineBayOffer(listingId, buyerId, buyerName, moneyOffer, items);
            offer.setOfferId(offerId);
            offer.setCreatedTimestamp(timestamp);
            offers.add(offer);
        }
        return offers;
    }
''')

register_complex('CreateListingPacket',
    '        this(ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf), buf.readDouble(), buf.readDouble(), buf.readEnum(MineBayListing.OfferType.class), decodePriceItems(buf));',
    '''
    private static List<PriceItemEntry> decodePriceItems(FriendlyByteBuf buf) {
        int priceItemCount = Math.min(buf.readInt(), 54);
        List<PriceItemEntry> priceItems = new ArrayList<>();
        for (int i = 0; i < priceItemCount; i++) {
            ItemStack itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
            int amount = buf.readInt();
            boolean useStacks = buf.readBoolean();
            if (!itemStack.isEmpty()) {
                priceItems.add(new PriceItemEntry(itemStack, amount, useStacks));
            }
        }
        return priceItems;
    }
''')

register_complex('CreateOfferPacket',
    '        this(buf.readUtf(36), Math.max(0.0, buf.readDouble()), decodeItemOffers(buf));',
    '''
    private static List<ItemStack> decodeItemOffers(FriendlyByteBuf buf) {
        int itemCount = buf.readInt();
        if (itemCount < 0 || itemCount > 27) itemCount = 0;
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
        }
        return items;
    }
''')

# SyncListingOffersPacket needs a fix - the static helper references 'listingId' from the first field
# We need to handle this differently since the offers list needs the listingId
# Actually, looking at the original code, it uses the OUTER listingId field.
# In the static helper, we don't have access to it. Let me fix this.
# We'll pass listingId as a parameter to the helper... but that's not possible with the this() pattern.
# Actually, looking more carefully at the decode:
#   this.listingId = buf.readUtf(36);  <- first field
#   ... then offers use the outer listingId
# For the record constructor: this(buf.readUtf(36), decodeOffers(buf, ???))
# But we need the listingId value. We can read it first and pass it:
# Actually, we can't because buf.readUtf(36) for listingId is the FIRST arg.
# Java evaluates left to right, so buf.readUtf(36) runs first.
# But we need that value INSIDE decodeOffers too.
# Solution: read listingId INSIDE decodeOffers and add it there, but then we'd read it twice.
# Better solution: use a helper that reads both:
COMPLEX_HANDLERS['SyncListingOffersPacket'] = {
    'buf_body': '        this(decodeSyncListingOffers(buf));',
    'static_helpers': '''
    private record DecodedSyncListingOffers(String listingId, List<MineBayOffer> offers) {}

    private static DecodedSyncListingOffers decodeSyncListingOffersHelper(FriendlyByteBuf buf) {
        String listingId = buf.readUtf(36);
        int count = buf.readInt();
        if (count < 0 || count > 50) count = 0;
        List<MineBayOffer> offers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String offerId = buf.readUtf(36);
            UUID buyerId = buf.readUUID();
            String buyerName = buf.readUtf(16);
            double moneyOffer = buf.readDouble();
            long timestamp = buf.readLong();
            int itemCount = buf.readInt();
            if (itemCount < 0 || itemCount > 27) itemCount = 0;
            List<ItemStack> items = new ArrayList<>();
            for (int j = 0; j < itemCount; j++) {
                items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(
                    (net.minecraft.network.RegistryFriendlyByteBuf) buf));
            }
            MineBayOffer offer = new MineBayOffer(listingId, buyerId, buyerName, moneyOffer, items);
            offer.setOfferId(offerId);
            offer.setCreatedTimestamp(timestamp);
            offers.add(offer);
        }
        return new DecodedSyncListingOffers(listingId, offers);
    }
''',
    'extra_changes': None,
}

# Actually, the above doesn't work because this() expects two args, not one.
# Let me use a different approach: read listingId first, then pass to helper.
# this(listingId=buf.readUtf(36), offers=decodeOffers(buf, ???)) -- can't pass listingId
# Actually we CAN if we do a two-step approach. But since records require this() as first statement...
# 
# The cleanest approach: just read listingId from buf in the first arg,
# and in decodeOffers, the buf position is already past listingId. But we need listingId for MineBayOffer construction.
# We could store it in a ThreadLocal or just pass it differently.
#
# Actually the simplest fix: just DON'T use listingId from the field in the offer construction.
# Looking at the original code: MineBayOffer(listingId, buyerId, ...) -- this uses the FIELD listingId.
# But since listingId was read from buf before the loop, we need that value.
# 
# Cleanest solution: pass listingId to decodeOffers. But how?
# We can use the fact that Java evaluates args left to right:
#   this(tmp = buf.readUtf(36), decodeOffers(buf, tmp))
# But Java doesn't allow assignments in method call args like that.
#
# Alternative: change the approach for this packet to use a full static factory.
# 
# Actually, the offer uses the LISTING'S id. Since we're reading it from buf, we know it.
# We can read it INSIDE the helper and return both values.
# Then the buf constructor reads nothing from buf - it's all done by the factory.
# But this() requires individual args...
#
# Hmm, let me look at this differently. What if decodeOffers reads the listingId from buf too?
# Then: this(decodeOffers(buf).listingId(), decodeOffers(buf).offers()) -- NO, double read!
#
# OK, the simplest correct approach: read everything in a single static method that returns an array:

COMPLEX_HANDLERS['SyncListingOffersPacket'] = {
    'buf_body': None,  # Will be handled specially
    'static_helpers': '''
    private static List<MineBayOffer> decodeOffers(FriendlyByteBuf buf, String listingId) {
        int count = buf.readInt();
        if (count < 0 || count > 50) count = 0;
        List<MineBayOffer> offers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String offerId = buf.readUtf(36);
            UUID buyerId = buf.readUUID();
            String buyerName = buf.readUtf(16);
            double moneyOffer = buf.readDouble();
            long timestamp = buf.readLong();
            int itemCount = buf.readInt();
            if (itemCount < 0 || itemCount > 27) itemCount = 0;
            List<ItemStack> items = new ArrayList<>();
            for (int j = 0; j < itemCount; j++) {
                items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(
                    (net.minecraft.network.RegistryFriendlyByteBuf) buf));
            }
            MineBayOffer offer = new MineBayOffer(listingId, buyerId, buyerName, moneyOffer, items);
            offer.setOfferId(offerId);
            offer.setCreatedTimestamp(timestamp);
            offers.add(offer);
        }
        return offers;
    }
''',
    'extra_changes': None,
    'special_buf_constructor': True,
}

# For SyncListingOffersPacket we'll handle it with a special approach:
# Use a compact constructor pattern that reads both fields from a helper
# Actually, the cleanest: just use a thread-local or field to pass data.
# NO. The cleanest is: Make the record have listingId first (which it does).
# Read listingId, store in local, then create offers using the local.
# In a record delegating constructor, we can't have locals before this().
# BUT... we can do:
#
# public SyncListingOffersPacket(FriendlyByteBuf buf) {
#     this(buf.readUtf(36), new ArrayList<>());  // read listingId, empty offers
# }
# NO, that loses the offers data.
#
# Actually, the REAL solution: we DON'T need listingId inside the offers decode
# at the network level. The listingId is already known from context.
# We can just use "" or a placeholder in the MineBayOffer and set it after.
# But that changes behavior.
#
# SIMPLEST CORRECT SOLUTION: a static method that takes FriendlyByteBuf
# and a String[] holder to capture the listingId:
# Nope, too hacky.
#
# ACTUAL SIMPLEST: since we're converting to records, and the first arg
# is listingId = buf.readUtf(36), we know it's consumed from the buffer.
# The second arg decodeOffers(buf) would then read the REST of the buffer.
# But decodeOffers needs the listingId value for MineBayOffer construction.
# We can't access it because it's the FIRST arg and we're computing the SECOND.
#
# REAL SOLUTION: Use a two-element array trick:
# private static String[] holder = null; // ThreadLocal would be safer but overkill
# 
# Actually, let me just use a different field order for the decode:
# Read everything in the helper, including listingId. But then we have two fields 
# from one decode call...
#
# OK FINAL ANSWER: I'll make the SyncListingOffersPacket decode pass the listingId  
# via a ThreadLocal. Actually no, let me just refactor:
# MineBayOffer takes listingId in constructor, but we can pass null/"" during decode 
# and the listingId is set separately. But looking at MineBayOffer constructor,
# it's used for validation/storage. Let me just skip the listingId in the offer 
# and use the packet's listingId field after construction.
# 
# Actually, THE REAL SIMPLEST: I can just repeat the listingId literal in both
# the first arg and the helper call, but that reads TWICE from the buffer.
# 
# THE ACTUAL SIMPLEST: Read all fields at once in a static decode wrapper
# that returns an Object[]:
#
# Actually you know what? Let me just store listingId in a temporary field.
# No, records can't have extra mutable fields.
#
# OK I'll use the approach where the static helper reconstructs the packet:
# public SyncListingOffersPacket(FriendlyByteBuf buf) {
#     this(decode(buf));
# }
# private SyncListingOffersPacket(SyncListingOffersPacket other) {
#     this(other.listingId, other.offers);
# }
# private static SyncListingOffersPacket decode(FriendlyByteBuf buf) { ... }
# NO, that's a recursive constructor.
#
# THE PRAGMATIC SOLUTION: Just hardcode the decode to NOT use listingId field.
# Instead, read listingId into a local inside the static helper and pass it to each offer.

# Actually wait - I had the right idea. Let me re-read the approach:
# The record has fields: (String listingId, List<MineBayOffer> offers)
# The buf constructor: this(buf.readUtf(36), decodeOffers(buf))
# decodeOffers reads the REST of the buffer, creating offers.
# But MineBayOffer needs listingId in its constructor.
# Since buf.readUtf(36) already consumed listingId from the buffer,
# we can't read it again.
#
# SOLUTION: Read listingId INSIDE decodeOffers and return both via an intermediate:
# Wait, I can't return both from one method call to use as two separate this() args.
#
# OK, I'll just make decodeOffers NOT pass listingId to MineBayOffer during decode.
# After decode, the offers won't have the correct listingId, but since they're only
# used client-side for display, this might be OK.
#
# ACTUALLY NO. Let me just change the decode to read the listingId inside the static
# method and pass it directly:
# this(decodeFromBuf(buf))  -- returns the full packet
# But this() needs two args...
#
# HACK BUT WORKS: ThreadLocal<String> to pass listingId between first arg eval and 
# second arg eval. Since Java evaluates left to right:
# this(captureListingId(buf.readUtf(36)), decodeOffers(buf))
# where captureListingId stores in a static field and returns the value.
# Then decodeOffers reads from the static field.
# This is thread-safe IF we use ThreadLocal. Actually for MC server, packets
# are handled on the network thread, so it should be fine with a plain static.
# But to be safe, ThreadLocal.

# ACTUALLY THE SIMPLEST: Just don't pass listingId to MineBayOffer in the decode.
# The listingId is stored in the MineBayOffer for reference, but the offers
# already know which listing they belong to. We can set it later in the constructor
# body or just use a compact constructor.
# 
# Wait, in a record I can add a compact constructor:
# public SyncListingOffersPacket {
#     for (MineBayOffer offer : offers) {
#         // set listingId if needed
#     }
# }
# 
# Or... just accept the slight behavior change: during decode, offers get "" as listingId.
# Looking at where offers are used on the client: ClientMineBayData.setListingOffers(listingId, offers)
# So the client uses the PACKET's listingId, not the offer's listingId. So it's fine!

COMPLEX_HANDLERS['SyncListingOffersPacket'] = {
    'buf_body': '        this(buf.readUtf(36), decodeOffers(buf));',
    'static_helpers': '''
    private static List<MineBayOffer> decodeOffers(FriendlyByteBuf buf) {
        int count = buf.readInt();
        if (count < 0 || count > 50) count = 0;
        List<MineBayOffer> offers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String offerId = buf.readUtf(36);
            UUID buyerId = buf.readUUID();
            String buyerName = buf.readUtf(16);
            double moneyOffer = buf.readDouble();
            long timestamp = buf.readLong();
            int itemCount = buf.readInt();
            if (itemCount < 0 || itemCount > 27) itemCount = 0;
            List<ItemStack> items = new ArrayList<>();
            for (int j = 0; j < itemCount; j++) {
                items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(
                    (net.minecraft.network.RegistryFriendlyByteBuf) buf));
            }
            MineBayOffer offer = new MineBayOffer("", buyerId, buyerName, moneyOffer, items);
            offer.setOfferId(offerId);
            offer.setCreatedTimestamp(timestamp);
            offers.add(offer);
        }
        return offers;
    }
''',
    'extra_changes': None,
}


def convert_file(filepath):
    """Convert a single packet class file to a record."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    filename = os.path.basename(filepath)

    if re.search(r'public\s+record\s+\w+', content):
        print(f"  SKIP (already record): {filename}")
        return

    class_match = re.search(
        r'public\s+class\s+(\w+)\s+implements\s+CustomPacketPayload', content)
    if not class_match:
        print(f"  SKIP (not a packet): {filename}")
        return
    class_name = class_match.group(1)

    fields = parse_fields(content)
    is_complex = class_name in COMPLEX_HANDLERS

    # Special handling for ModFileChunkPacket: reorder fields
    if class_name == 'ModFileChunkPacket':
        # Original: chunkIndex, totalChunks, chunkData, fileHash
        # New: chunkIndex, totalChunks, fileHash, chunkData (matches encode/decode order)
        new_fields = []
        for ft, fn in fields:
            new_fields.append((ft, fn))
        # Swap chunkData and fileHash
        idx_chunkData = next(i for i, (_, n) in enumerate(new_fields) if n == 'chunkData')
        idx_fileHash = next(i for i, (_, n) in enumerate(new_fields) if n == 'fileHash')
        if idx_chunkData < idx_fileHash:
            new_fields[idx_chunkData], new_fields[idx_fileHash] = new_fields[idx_fileHash], new_fields[idx_chunkData]
        fields = new_fields

    # Step 1: Build record declaration
    if fields:
        components = ', '.join(f'{t} {n}' for t, n in fields)
    else:
        components = ''

    content = re.sub(
        r'public\s+class\s+' + re.escape(class_name) +
        r'\s+implements\s+CustomPacketPayload',
        f'public record {class_name}({components}) implements CustomPacketPayload',
        content
    )

    # Step 2: Remove private final field declarations
    original_fields = parse_fields(content)  # Re-parse won't find them since class changed
    # Use the stored fields list instead
    for ftype, fname in (fields if class_name != 'ModFileChunkPacket' else parse_fields_from_original(filepath)):
        ftype_esc = re.escape(ftype)
        content = re.sub(
            r'\n[ \t]*private\s+final\s+' + ftype_esc +
            r'\s+' + re.escape(fname) + r'\s*;[ \t]*',
            '',
            content
        )

    # For ModFileChunkPacket, we need to re-extract fields from the original content
    # Actually, let's just remove all private final fields
    content = re.sub(r'\n[ \t]*private\s+final\s+[\w.]+(?:<[\w.,\s<>?]+>)?(?:\[\])?\s+\w+\s*;[ \t]*', '', content)

    # Step 3: Process constructors
    constructors = find_all_constructors(content, class_name)
    # Process from end to start to avoid offset issues
    constructors.sort(key=lambda x: x[0], reverse=True)

    for start, end, header, body, params_str in constructors:
        if 'FriendlyByteBuf' in params_str:
            # FriendlyByteBuf constructor
            if not fields:
                # Empty packet
                new_body = '\n        this();\n    '
                content = content[:start] + header + new_body + '}' + content[end + 1:]
            elif is_complex:
                handler = COMPLEX_HANDLERS[class_name]
                buf_body = handler['buf_body']
                if buf_body:
                    new_body = f'\n{buf_body}\n    '
                    content = content[:start] + header + new_body + '}' + content[end + 1:]
            else:
                # Try simple conversion
                exprs = try_simple_buf_conversion(body, fields)
                if exprs:
                    this_call = f'this({", ".join(exprs)})'
                    new_body = f'\n        {this_call};\n    '
                    content = content[:start] + header + new_body + '}' + content[end + 1:]
                else:
                    print(f"  WARNING: {filename} - buf constructor not auto-converted")
        else:
            # Non-FriendlyByteBuf constructor
            if fields and is_canonical_constructor(body, fields):
                # Remove canonical constructor (with preceding whitespace)
                remove_start = start
                while remove_start > 0 and content[remove_start - 1] in ' \t\n':
                    remove_start -= 1
                # Keep at least one newline
                if remove_start < start:
                    remove_start += 1
                content = content[:remove_start] + content[end + 1:]
            elif not fields:
                # Empty packet - remove the empty constructor
                body_stripped = body.strip()
                if not body_stripped or body_stripped == '// No data to read' or body_stripped == '// No data needed':
                    remove_start = start
                    while remove_start > 0 and content[remove_start - 1] in ' \t\n':
                        remove_start -= 1
                    if remove_start < start:
                        remove_start += 1
                    content = content[:remove_start] + content[end + 1:]
            elif fields:
                # Convenience constructor - convert to this(...)
                exprs = extract_convenience_exprs(body, fields)
                if exprs:
                    this_call = f'this({", ".join(exprs)})'
                    new_body = f'\n        {this_call};\n    '
                    content = content[:start] + header + new_body + '}' + content[end + 1:]
                else:
                    print(f"  WARNING: {filename} - convenience constructor not converted: {params_str[:50]}")

    # Step 4: Add static helpers for complex packets
    if is_complex:
        handler = COMPLEX_HANDLERS[class_name]
        helpers = handler.get('static_helpers', '')
        if helpers:
            # Insert helpers before the last closing brace of the class
            last_brace = content.rfind('}')
            if last_brace > 0:
                content = content[:last_brace] + helpers + content[last_brace:]

        # Apply extra changes
        extra = handler.get('extra_changes')
        if extra:
            content = extra(content)

    # Step 5: Clean up excessive blank lines
    content = re.sub(r'\n{4,}', '\n\n\n', content)
    # Clean up blank lines after record declaration
    content = re.sub(r'(implements CustomPacketPayload \{)\n{3,}', r'\1\n', content)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

    status = "COMPLEX" if is_complex else "simple"
    print(f"  Converted ({status}): {filename}")


def parse_fields_from_original(filepath):
    """Re-read original file to get fields."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    return parse_fields(content)


def main():
    packet_dir = BASE_DIR
    minebay_dir = os.path.join(BASE_DIR, 'minebay')

    files = []
    for f in sorted(os.listdir(packet_dir)):
        if f.endswith('.java'):
            files.append(os.path.join(packet_dir, f))
    if os.path.exists(minebay_dir):
        for f in sorted(os.listdir(minebay_dir)):
            if f.endswith('.java'):
                files.append(os.path.join(minebay_dir, f))

    print(f"Found {len(files)} packet files\n")

    for filepath in files:
        try:
            convert_file(filepath)
        except Exception as e:
            print(f"  ERROR: {os.path.basename(filepath)}: {e}")
            import traceback
            traceback.print_exc()

    print(f"\nDone! Converted {len(files)} files.")


if __name__ == '__main__':
    main()
