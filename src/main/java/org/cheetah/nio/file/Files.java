/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.nio.file;

import java.io.IOException;
import java.nio.file.attribute.FileAttribute;
import java.security.SecureRandom;

/**
 *
 * @author phs
 */
public class Files {

    public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs)
            throws IOException {
        if (prefix == null) {
            prefix = "";
        }
        if (suffix == null) {
            suffix = ".tmp";
        }

        SecureRandom random = new SecureRandom();
        long n = random.nextLong();
        n = (n == Long.MIN_VALUE) ? 0 : Math.abs(n);
        Path dir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path name = dir.resolve(prefix + Long.toString(n) + suffix);

        name.toFile().createNewFile();

        return name;
    }

    public static DirectoryStream<Path> newDirectoryStream(Path path) {
        DirectoryStream<Path> directoryStream = new DirectoryStream();

        /*
        File dir = path.toFile();

        System.out.println("dir: " + dir);

        File[] filesList = dir.listFiles();

        System.out.println("START LIST");

        if (filesList != null) {
            for (File file : filesList) {
                System.out.println(file.getName());
                directoryStream.add(Paths.get(file.getName()));
            }
        }

        System.out.println(" END  LIST: " + directoryStream.list.size());

*/
        return directoryStream;
    }

    public static boolean exists(Path path, LinkOption... options) {

        return path.toFile().exists();
    }

    public static boolean isRegularFile(Path path, LinkOption... options) {
    //    System.out.println("isRegularFile: " + path.toString() + ": " + path.toFile().isFile());

        return path.toFile().isFile();
    }

    public static boolean isSymbolicLink(Path path, LinkOption... options) {
        return false;
    }

    public static boolean isDirectory(Path path, LinkOption... options) {

    //    System.out.println("isDirectory: " + path.toString() + ": " + path.toFile().isDirectory());

        return path.toFile().isDirectory();
    }

    public static boolean delete(Path path, LinkOption... options) throws IOException {
        return path.toFile().delete();
    }

    public static boolean createDirectory(Path path, LinkOption... options) throws IOException {
        return path.toFile().mkdir();
    }

    public static boolean move(Path sourcePath, Path destPath) throws IOException {
        return sourcePath.toFile().renameTo(destPath.toFile());
    }
}
