$ruleText = @"
- **Nested Widget Focus Bouncing**: When a `Screen` processes `mouseClicked`, it temporarily clears focus (`setFocused(false)`) before setting it to the clicked widget (`setFocused(true)`). For custom widgets containing nested `EditBox`es, this focus bounce will aggressively unfocus the nested `EditBox` if it isn't properly restored. To fix this, use an `isPendingSearchBoxFocus` boolean flag that is set to true when the nested `EditBox` is clicked, and restore the `EditBox`'s focus inside the widget's `setFocused(boolean)` override if the flag is true.
"@

$files = Get-ChildItem -Recurse -Filter *GEMINI.md

foreach ($file in $files) {
    # Ignore node_modules, build directories, etc., just looking in root is usually enough but recursive is safe
    $content = Get-Content -Raw -Encoding UTF8 $file.FullName
    
    if ($content -match "isPendingSearchBoxFocus") {
        Write-Host "Rule already exists in $($file.Name)"
        continue
    }

    $newContent = $content + "`n" + $ruleText + "`n"
    Set-Content -Path $file.FullName -Value $newContent -Encoding UTF8
    Write-Host "Injected global UI rule into $($file.Name)"
}
