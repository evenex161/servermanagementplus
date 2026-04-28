"""Apply 1.20.1 fabric-specific fixes to fabric/src/main/java/com/servermanagement"""
import pathlib, re, sys

ROOT = pathlib.Path('fabric/src/main/java/com/servermanagement')

def fix(p, fn):
    t0 = p.read_text(encoding='utf-8')
    t = fn(t0)
    if t != t0:
        p.write_text(t, encoding='utf-8')
        print('patched', p)

# 1. renderBackground 4-arg → 1-arg
def fix_render_bg(t):
    return re.sub(r'this\.renderBackground\(([^,)]+),\s*[^,)]+,\s*[^,)]+,\s*[^)]+\)', r'this.renderBackground(\1)', t)

# 2. ResourceLocation.parse(X) → new ResourceLocation(X)
def fix_rl_parse(t):
    return re.sub(r'(?:net\.minecraft\.resources\.)?ResourceLocation\.parse\(', 'new ResourceLocation(', t)

# 3. instanceof ServerPlayer joiningPlayer when var already ServerPlayer
def fix_instanceof(t):
    # Remove the pattern variable since the source is already ServerPlayer
    # e.g., `if (!(player instanceof ServerPlayer joiningPlayer)) return;` followed by joiningPlayer use
    # Safer: replace the if with comment-level - actually it's a hard error. Forge equivalent likely
    # has it as something else. Just rename in place: use direct cast.
    # The error suggests javac strict mode complains. Try changing pattern var to a different name
    # and adding explicit cast, or just removing the redundant instanceof.
    # Simplest fix: javac flag --release 17 should accept this. But error says it IS an error.
    # Looking at message: "expression type ServerPlayer is a subtype of pattern type ServerPlayer"
    # javac in some versions errors on this. Solution: rename or use explicit assignment.
    return t

for p in ROOT.rglob('*.java'):
    fix(p, lambda t: fix_render_bg(fix_rl_parse(t)))
