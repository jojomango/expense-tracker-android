# R8 規則。目前用到的函式庫（Room、Hilt、WorkManager、kotlinx-serialization、Compose）
# 都自帶 consumer rules，這裡刻意保持空白——只有在 release 版實測發現被誤刪的類別時才加，
# 並註明是為了哪個問題加的。

# 保留行號，讓 Play Console 的當機堆疊（Android vitals）對得回原始碼
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
