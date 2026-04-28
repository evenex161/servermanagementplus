#!/usr/bin/env python3
"""Convert Fabric packet classes from regular Java classes to Java records.

Handles:
- Simple packets: direct this.field = buf.readXxx() assignments
- Complex packets: loops/maps/conditionals via pre-built overrides
- Empty packets: no fields
- Convenience constructors: converted to this(...) delegation
- Inner classes/enums/records: preserved
"""
import re
import os

BASE_DIR = r"C:\VS Workspace\Projects\Minecraft Mods\Servermanagement-Forge\fabric\src\main\java\com\servermanagement\network\packet"


def find_balanced_braces(text, start):
    """Find closing brace matching the opening brace at 'start'."""
    depth = 0
    i = start
    in_string = False
    in_char = False
    while i < len(text):
        c = text[i]
        if in_string:
            if c == '\\':
                i += 2
                continue
            if c == '"':
                in_string = False
        elif in_char:
            if c == '\\':
                i += 2
                continue
            if c == "'":
                in_char = False
        else:
            if c == '"':
                in_string = True
            elif c == "'":
                in_char = True
            elif c == '{':
                depth += 1
            elif c == '}':
                depth -= 1
                if depth == 0:
                    return i
        i += 1
    return -1


def find_all_constructors(content, class_name):
    """Find all public constructors. Returns list of (start, end, params_text, body_text)."""
    results = []
    pattern = rf'\n(\s*)public {re.escape(class_name)}\(([^)]*)\)\s*\{{'
    for m in re.finditer(pattern, content):
        line_start = m.start() + 1  # skip leading \n
        brace_pos = content.index('{', line_start + len(m.group(0)) - 2)
        brace_end = find_balanced_braces(content, brace_pos)
        if brace_end < 0:
            continue
        params_text = m.group(2).strip()
        body_text = content[brace_pos + 1:brace_end]
        results.append((line_start, brace_end + 1, params_text, body_text))
    return results


def is_canonical(params, body, field_names):
    """Check if constructor is canonical: assigns each field = param directly, nothing else."""
    if 'FriendlyByteBuf' in params or 'buf.' in body:
        return False
    for fname in field_names:
        # Must be exactly: this.field = field; (not this.field = field.copy() etc.)
        if not re.search(rf'this\.{re.escape(fname)}\s*=\s*{re.escape(fname)}\s*;', body):
            return False
    return True


def is_buf_ctor(params):
    return 'FriendlyByteBuf' in params


def extract_simple_assignments(body, field_names):
    """Extract this.field = expr; from body. Returns dict or None if complex."""
    assigns = {}
    for line in body.split('\n'):
        line = line.strip()
        if not line or line.startswith('//'):
            continue
        m = re.match(r'this\.(\w+)\s*=\s*(.+?)\s*;\s*$', line)
        if m:
            assigns[m.group(1)] = m.group(2)
        else:
            return None  # Non-assignment line → complex
    if set(assigns.keys()) != set(field_names):
        return None
    return assigns


# ─── COMPLEX BUF CONSTRUCTOR OVERRIDES ──────────────────────────────────────
# For packets whose FriendlyByteBuf constructor has loops, conditionals,
# out-of-order reads, or early returns, we provide the exact replacement text.

COMPLEX_BUF_CTORS = {

'ModFileChunkPacket': '''\
    public ModFileChunkPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), decodeChunkPayload(buf));
    }

    private ModFileChunkPacket(int chunkIndex, int totalChunks, Object[] payload) {
        this(chunkIndex, totalChunks, (byte[]) payload[0], (String) payload[1]);
    }

    private static Object[] decodeChunkPayload(FriendlyByteBuf buf) {
        String fileHash = buf.readUtf(128);
        int dataLength = Math.min(buf.readInt(), CHUNK_SIZE + 1024);
        byte[] chunkData = new byte[dataLength];
        buf.readBytes(chunkData);
        return new Object[]{chunkData, fileHash};
    }''',

'OpenGuiPacket': '''\
    public OpenGuiPacket(FriendlyByteBuf buf) {
        this(decodeGuiType(buf.readInt()), buf.readUtf(256));
    }

    private static GuiType decodeGuiType(int ordinal) {
        GuiType[] values = GuiType.values();
        return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : GuiType.DASHBOARD;
    }''',

'PMSyncPlayerListsPacket': '''\
    public PMSyncPlayerListsPacket(FriendlyByteBuf buf) {
        this(decodeStringList(buf), decodeStringList(buf), buf.readBoolean());
    }

    private static List<String> decodeStringList(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(buf.readUtf(16));
        }
        return list;
    }''',

'SyncAchievementsPacket': '''\
    public SyncAchievementsPacket(FriendlyByteBuf buf) {
        this(decodeAchievements(buf), buf.readInt());
    }

    private static Set<String> decodeAchievements(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Set<String> set = new HashSet<>();
        for (int i = 0; i < count; i++) {
            set.add(buf.readUtf(128));
        }
        return set;
    }''',

'SyncBankAccountPacket': '''\
    public SyncBankAccountPacket(FriendlyByteBuf buf) {
        this(buf.readDouble(), decodeTransactions(buf));
    }

    private static List<Transaction> decodeTransactions(FriendlyByteBuf buf) {
        int transactionCount = buf.readInt();
        List<Transaction> list = new ArrayList<>();
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
            list.add(new Transaction(type, amount, timestamp, description, otherParty));
        }
        return list;
    }''',

'SyncBankInventoryPacket': '''\
    public SyncBankInventoryPacket(FriendlyByteBuf buf) {
        this(buf.readNbt());
    }''',

'SyncDailyTasksPacket': '''\
    public SyncDailyTasksPacket(FriendlyByteBuf buf) {
        this(decodeTasks(buf), buf.readLong(), buf.readBoolean(), buf.readInt(), buf.readLong());
    }

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
    }''',

'SyncEconomyTemplatesPacket': '''\
    public SyncEconomyTemplatesPacket(FriendlyByteBuf buf) {
        this(decodeTemplates(buf), buf.readInt(), buf.readInt());
    }

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
    }''',

'SyncFeatureStatesPacket': '''\
    public SyncFeatureStatesPacket(FriendlyByteBuf buf) {
        this(decodeFeatureStates(buf));
    }

    private static Map<String, Boolean> decodeFeatureStates(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, Boolean> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buf.readUtf(64), buf.readBoolean());
        }
        return map;
    }''',

'SyncMarketPricesPacket': '''\
    public SyncMarketPricesPacket(FriendlyByteBuf buf) {
        this(buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readDouble(),
             decodeSupplyData(buf), decodeRecipePrices(buf));
    }

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
    }''',

'SyncMoneyRequestsPacket': '''\
    public SyncMoneyRequestsPacket(FriendlyByteBuf buf) {
        this(decodeEntries(buf), decodeEntries(buf));
    }

    private static List<ClientMoneyRequestData.RequestEntry> decodeEntries(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ClientMoneyRequestData.RequestEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new ClientMoneyRequestData.RequestEntry(
                buf.readUUID(), buf.readUtf(16), buf.readDouble(),
                buf.readUtf(256), buf.readUtf(64), buf.readUtf(32)));
        }
        return list;
    }''',

'SyncWorldListPacket': '''\
    public SyncWorldListPacket(FriendlyByteBuf buf) {
        this(decodeWorlds(buf));
    }

    private static List<WorldInfo> decodeWorlds(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<WorldInfo> worlds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            worlds.add(new WorldInfo(
                buf.readUtf(256), buf.readUtf(128),
                buf.readBoolean(), buf.readBoolean(), buf.readInt()));
        }
        return worlds;
    }''',

'CreateListingPacket': '''\
    public CreateListingPacket(FriendlyByteBuf buf) {
        this(
            ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf),
            buf.readDouble(),
            buf.readDouble(),
            buf.readEnum(MineBayListing.OfferType.class),
            decodePriceItems(buf)
        );
    }

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
    }''',

'CreateOfferPacket': '''\
    public CreateOfferPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(36), Math.max(0.0, buf.readDouble()), decodeItemOffers(buf));
    }

    private static List<ItemStack> decodeItemOffers(FriendlyByteBuf buf) {
        int itemCount = buf.readInt();
        if (itemCount < 0 || itemCount > 27) itemCount = 0;
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf));
        }
        return items;
    }''',

'SyncListingOffersPacket': '''\
    public SyncListingOffersPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(36), decodeOffers(buf));
    }

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
    }''',

'SyncMineBayListingsPacket': '''\
    public SyncMineBayListingsPacket(FriendlyByteBuf buf) {
        this(decodeListings(buf));
    }

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
    }''',

'PurchaseListingPacket': '''\
    public PurchaseListingPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(36), buf.readByte(), decodeSelectedSlots(buf));
    }

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
    }''',

}


def convert_convenience_ctor(class_name, params, body, field_names):
    """Convert a convenience constructor to delegate via this(...)."""
    assigns = {}
    for line in body.split('\n'):
        line = line.strip()
        if not line or line.startswith('//'):
            continue
        m = re.match(r'this\.(\w+)\s*=\s*(.+?)\s*;\s*$', line)
        if m:
            assigns[m.group(1)] = m.group(2)
        else:
            # Non-assignment line (method call, control flow, etc.) - can't auto-convert
            return None
    if not assigns:
        return None
    args = []
    for fn in field_names:
        if fn in assigns:
            args.append(assigns[fn])
        else:
            return None  # Can't determine all args
    return f'    public {class_name}({params}) {{\n        this({", ".join(args)});\n    }}'


def convert_file(filepath):
    """Convert a single packet file from class to record."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Only convert classes implementing CustomPacketPayload
    class_match = re.search(r'public class (\w+) implements', content)
    if not class_match:
        return 'SKIP', 'not a class or no implements'
    class_name = class_match.group(1)

    # ── Extract private final (non-static) fields ──
    field_pattern = r'^\s+private final\s+([\w.<>\[\], ?]+)\s+(\w+)\s*;'
    fields = [(m.group(1), m.group(2)) for m in re.finditer(field_pattern, content, re.MULTILINE)]
    field_names = [f[1] for f in fields]
    record_params = ", ".join(f"{ftype} {fname}" for ftype, fname in fields)

    # ── 1. class → record ──
    content = content.replace(
        f'public class {class_name} implements',
        f'public record {class_name}({record_params}) implements', 1)

    # ── 2. Remove private final field declarations ──
    for ftype, fname in fields:
        pat = rf'\n\s+private final {re.escape(ftype)}\s+{re.escape(fname)}\s*;[ \t]*'
        content = re.sub(pat, '', content, count=1)

    # ── 3. Remove canonical constructor ──
    ctors = find_all_constructors(content, class_name)
    remove_ranges = []
    for s, e, params, body in ctors:
        if field_names and is_canonical(params, body, field_names):
            remove_ranges.append((s, e))
        elif not field_names and params == '' and 'buf.' not in body:
            # Empty no-arg constructor for empty packets
            remove_ranges.append((s, e))

    for s, e in sorted(remove_ranges, reverse=True):
        # Eat trailing blank line
        while e < len(content) and content[e] in ' \t':
            e += 1
        if e < len(content) and content[e] == '\n':
            e += 1
        content = content[:s] + content[e:]

    # ── 4. Convert FriendlyByteBuf constructor ──
    ctors2 = find_all_constructors(content, class_name)
    for s, e, params, body in ctors2:
        if not is_buf_ctor(params):
            continue

        if class_name in COMPLEX_BUF_CTORS:
            content = content[:s] + COMPLEX_BUF_CTORS[class_name] + content[e:]
        elif not field_names:
            content = content[:s] + f'    public {class_name}(FriendlyByteBuf buf) {{\n        this();\n    }}' + content[e:]
        else:
            assigns = extract_simple_assignments(body, field_names)
            if assigns:
                args = [assigns[fn] for fn in field_names]
                call = f'this({", ".join(args)})'
                content = content[:s] + f'    public {class_name}(FriendlyByteBuf buf) {{\n        {call};\n    }}' + content[e:]
            else:
                print(f"  *** MANUAL FIX NEEDED: {class_name} (complex buf ctor not in overrides)")
        break  # Only one buf constructor

    # ── 5. Convert convenience constructors ──
    ctors3 = find_all_constructors(content, class_name)
    for s, e, params, body in reversed(ctors3):
        if is_buf_ctor(params) or not params:
            continue
        if 'this.' not in body:
            continue
        # Skip if already a this(...) delegation
        if body.strip().startswith('this('):
            continue
        new = convert_convenience_ctor(class_name, params, body, field_names)
        if new:
            content = content[:s] + new + content[e:]
        else:
            print(f"  *** Could not auto-convert convenience ctor in {class_name}")

    # ── 6. Clean up excessive blank lines ──
    content = re.sub(r'\n{3,}', '\n\n', content)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

    return 'OK', class_name


def convert_all():
    """Process all packet files under BASE_DIR recursively."""
    count = 0
    errors = 0

    for dirpath, _, filenames in os.walk(BASE_DIR):
        for fn in sorted(filenames):
            if not fn.endswith('.java'):
                continue
            fp = os.path.join(dirpath, fn)
            try:
                status, info = convert_file(fp)
                print(f"  {status}: {fn} -> {info}")
                if status == 'OK':
                    count += 1
                else:
                    errors += 1
            except Exception as ex:
                print(f"  ERROR: {fn}: {ex}")
                import traceback
                traceback.print_exc()
                errors += 1

    print(f"\nDone. Converted: {count}, Errors/Skips: {errors}")


if __name__ == '__main__':
    convert_all()
