import re, sys

MINE = open("sharedUi/src/jvmSharedMain/kotlin/com/manikdev/bit/ui/screens/HomeScreen.kt", encoding="utf-8").read()
SUR = open("/scratch/work/Sur/composeApp/src/commonMain/kotlin/com/manikdev/spotify/nightly/ui/component/EchoForYouSection.kt", encoding="utf-8").read()

def fn_body(src, signature):
    """The exact body of one function, by brace matching from its signature."""
    i = src.index(signature)
    j = src.index("{", i)
    depth = 0
    for k in range(j, len(src)):
        if src[k] == "{":
            depth += 1
        elif src[k] == "}":
            depth -= 1
            if depth == 0:
                return src[i:k + 1]
    raise ValueError(signature)

row = fn_body(MINE, "private fun SurShelfRow(")
card = fn_body(MINE, "private fun SurShelfCard(")
sur_row = SUR   # the whole upstream file: every value below is one of its literals
sur_card = SUR

checks = [
    ("shelf title uses titleLarge",            "typography.titleLarge" in row and "typography.titleLarge" in sur_row),
    ("shelf title is Bold",                    "fontWeight = FontWeight.Bold" in row and "fontWeight = FontWeight.Bold" in sur_row),
    ("shelf title padding h=15dp v=8dp",       "padding(horizontal = 15.dp, vertical = 8.dp)" in row and "padding(horizontal = 15.dp, vertical = 8.dp)" in sur_row),
    ("row contentPadding h=15dp",              "contentPadding = PaddingValues(horizontal = 15.dp)" in row and "contentPadding = PaddingValues(horizontal = 15.dp)" in sur_row),
    ("row spacedBy 12dp",                      "Arrangement.spacedBy(12.dp)" in row and "Arrangement.spacedBy(12.dp)" in sur_row),
    ("first row cards 200dp",                  "if (shelfIndex == 0) 200.dp else 148.dp" in row and "if (index == 0) 200.dp else 148.dp" in sur_row),
    ("other row cards 148dp",                  "148.dp" in row and "148.dp" in sur_row),
    ("gap after a row is 8dp",                 "Spacer(Modifier.height(8.dp))" in row and "Spacer(modifier = Modifier.height(8.dp))" in sur_row),
    ("empty shelf draws nothing",              "if (shelf.items.isEmpty()) return" in row and "if (cards.isEmpty()) return" in sur_row),
    ("card is as wide as the row says",        ".width(width)" in card and ".width(width)" in sur_card),
    ("sleeve is square",                       "aspectRatio(1f)" in card and "aspectRatio(1f)" in sur_card),
    ("sleeve radius 12dp",                     "RoundedCornerShape(12.dp)" in card and "RoundedCornerShape(12.dp)" in sur_card),
    ("sleeve sits on surfaceVariant",          "surfaceVariant" in card and "surfaceVariant" in sur_card),
    ("image fills the sleeve",                 "fillMaxWidth().aspectRatio(1f)" in card and "fillMaxWidth().aspectRatio(1f)" in sur_card),
    ("image sets no contentScale",             "ContentScale" not in card and "contentScale" not in sur_card),
    ("6dp between sleeve and title",           "Spacer(Modifier.height(6.dp))" in card and "height(6.dp)" in sur_card),
    ("card title bodyMedium",                  "typography.bodyMedium" in card and "typography.bodyMedium" in sur_card),
    ("card title SemiBold",                    "fontWeight = FontWeight.SemiBold" in card and "fontWeight = FontWeight.SemiBold" in sur_card),
    ("card title one line",                    "maxLines = 1" in card and "maxLines = 1" in sur_card),
    ("card title ellipsises",                  "overflow = TextOverflow.Ellipsis" in card and "overflow = TextOverflow.Ellipsis" in sur_card),
    ("credit is bodySmall",                    "typography.bodySmall" in card and "typography.bodySmall" in sur_card),
    ("credit is onSurfaceVariant",             "onSurfaceVariant" in card and "onSurfaceVariant" in sur_card),
    ("card carries no explicit title colour",  "color = MaterialTheme.colorScheme.onBackground" not in card),
]

ok = 0
for name, passed in checks:
    print(("PASS  " if passed else "FAIL  ") + name)
    ok += 1 if passed else 0
print(f"\n{ok}/{len(checks)} checks pass")
sys.exit(0 if ok == len(checks) else 1)
