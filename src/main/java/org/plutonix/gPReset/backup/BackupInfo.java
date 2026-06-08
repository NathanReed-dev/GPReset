package org.plutonix.gPReset.backup;

import java.io.File;

public record BackupInfo (
    String name,
    long size,
    long lastModified,
    File file
    ){}
