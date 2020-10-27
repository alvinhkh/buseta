package com.alvinhkh.buseta.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import timber.log.Timber;

public class ZipUtil {
    public static void decompress(File zipFile) {
        try {
            FileInputStream fin = new FileInputStream(zipFile);
            ZipInputStream zin = new ZipInputStream(fin);
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.isDirectory()) {
                    File f = new File(zipFile.getParent() + File.separator + ze.getName());
                    if (!f.isDirectory()) {
                        f.mkdirs();
                    }
                } else {
                    if (ze.getName().startsWith(".")) continue;
                    Timber.d("ZIP: %s", zipFile.getParent() + File.separator + ze.getName());
                    FileOutputStream fout = new FileOutputStream(zipFile.getParent() + File.separator + ze.getName());
                    final int BUFFER = 2048;
                    byte data[] = new byte[BUFFER];
                    int count;
                    while((count = zin.read(data, 0, BUFFER)) != -1) {
                        fout.write(data, 0, count);
                    }
                    zin.closeEntry();
                    fout.close();
                }

            }
            zin.close();
        } catch(Exception e) {
            Timber.e(e);
        }
    }
}
