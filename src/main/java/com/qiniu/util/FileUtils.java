package com.qiniu.util;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class FileUtils {

    private static final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
    private static final FileNameMap fileNameMap = URLConnection.getFileNameMap();

    public static String contentType(String path) {
        String type = fileNameMap.getContentTypeFor(path);
        if (type == null) {
            type = mimetypesFileTypeMap.getContentType(path);
            if ("application/octet-stream".equals(type)) {
                try {
                    type = Files.probeContentType(Paths.get(path));
                    if (type == null) return "application/octet-stream";
                    else return type;
                } catch (IOException e) {
                    return "application/octet-stream";
                }
            } else {
                return type;
            }
        } else {
            return type;
        }
    }

    public static String contentType(File file) {
        String type = fileNameMap.getContentTypeFor(file.getPath());
        if (type == null) {
            type = mimetypesFileTypeMap.getContentType(file);
            if ("application/octet-stream".equals(type)) {
                try {
                    type = Files.probeContentType(file.toPath());
                    if (type == null) return "application/octet-stream";
                    else return type;
                } catch (IOException e) {
                    return "application/octet-stream";
                }
            } else {
                return type;
            }
        } else {
            return type;
        }
    }

    public static final String pathSeparator = System.getProperty("file.separator");
    public static final String userHomeStartPath = "~" + pathSeparator;
    public static final String currentPath = "." + pathSeparator;
    public static final String parentPath = ".." + pathSeparator;
    public static final String userHome = System.getProperty("user.home");

    public static String convertToRealPath(String filepath) throws IOException {
        if (filepath == null || "".equals(filepath)) throw new IOException("the path is empty.");
        if (filepath.startsWith(userHomeStartPath)) {
            return String.join("", userHome, filepath.substring(1));
        }
        if (filepath.startsWith("\\~")) { // 转义字符的路径
            return new File(currentPath + filepath.substring(1)).getCanonicalPath();
        }
        if (filepath.contains("\\~")) { // 转义字符的路径
            return new File(filepath.replace("\\~", "~")).getCanonicalPath();
        } else {
            return new File(filepath).getCanonicalPath();
        }
    }

    public static String rmPrefix(String prefix, String name) throws IOException {
        if (name == null) throw new IOException("empty filename.");
        if (prefix == null || "".equals(prefix) || name.length() < prefix.length()) return name;
        return String.join("", name.substring(0, prefix.length()).replace(prefix, ""),
                name.substring(prefix.length()));
    }

    public static String addSuffix(String name, String suffix) {
        return String.join("", name, suffix);
    }

    public static String addPrefix(String prefix, String name) {
        return String.join("", prefix, name);
    }

    public static String addPrefixAndSuffixKeepExt(String prefix, String name, String suffix) {

        return String.join("", prefix, addSuffixKeepExt(name, suffix));
    }

    public static String addSuffixKeepExt(String name, String suffix) {

        return addSuffixWithExt(name, suffix, null);
    }

    public static String addPrefixAndSuffixWithExt(String prefix, String name, String suffix, String ext) {
        return String.join("", prefix, addSuffixWithExt(name, suffix, ext));
    }

    public static String replaceExt(String name, String ext) {
        return addSuffixWithExt(name, "", ext);
    }

    public static String addSuffixWithExt(String name, String suffix, String ext) {
        if (name == null) return null;
        String[] items = getNameItems(name);
        if (ext != null && !"".equals(ext)) {
            return String.join("", items[0], suffix, ".", ext);
        } else if (items[1] == null || "".equals(items[1])) {
            return String.join("", items[0], suffix);
        } else {
            return String.join("", items[0], suffix, "." + items[1]);
        }
    }

    public static String[] getNameItems(String name) {
        String[] items = name.split("\\.");
        if (items.length < 2) {
            return new String[]{items[0], ""};
        }
        StringBuilder shortName = new StringBuilder();
        for (int i = 0; i < items.length - 1; i++) {
            shortName.append(items[i]).append(".");
        }
        return new String[]{shortName.substring(0, shortName.length() - 1), items[items.length - 1]};
    }

    public static List<File> getFiles(File directory, boolean checkText) throws IOException {
        File[] fs = directory.listFiles();
        if (fs == null) throw new IOException("The current path you gave may be incorrect: " + directory);
        List<File> files = new ArrayList<>();
//        Objects.requireNonNull(directory.listFiles());
        for(File f : fs) {
            if (f.isDirectory()) {
                files.addAll(getFiles(f, checkText));
            } else {
                if (checkText) {
                    String type = FileUtils.contentType(f);
                    if (type.startsWith("text") || type.equals("application/octet-stream")) {
                        files.add(f);
                    }
                } else {
                    files.add(f);
                }
            }
        }
        return files;
    }

    public static boolean mkDirAndFile(File filePath) throws IOException {
        File parent = filePath.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                if (!parent.mkdirs()) return false;
            }
        }
        return filePath.exists() || filePath.createNewFile() || filePath.createNewFile();
    }

    public static boolean createIfNotExists(File file) throws IOException {
        if (file == null) return false;
        if (file.exists()) {
            return true;
        } else {
            boolean success = false;
            IOException exception = null;
            try {
                success = file.createNewFile();
            } catch (IOException e) {
                exception = e;
            }
            if (success) {
                return true;
            } else {
                if (exception == null) exception = new IOException("create file: " + file.getPath() + " failed.");
                throw exception;
            }
        }
    }

    public static void randomModify(String filePath, String oldStr, String newStr) throws IOException {
        ///定义一个随机访问文件类的对象
        RandomAccessFile raf = null;
        try {
            //初始化对象,以"rw"(读写方式)访问文件
            raf = new RandomAccessFile(filePath, "rw");
            //临时变量,存放每次读出来的文件内容
            String line;
            // 记住上一次的偏移量
            long lastPoint = 0;
            //循环读出文件内容
            while ((line = raf.readLine()) != null) {
                // 文件当前偏移量返回文件记录指针的当前位置
                final long point = raf.getFilePointer();
                // 查找要替换的内容
                if (line.contains(oldStr)) {
                    //修改内容,line读出整行数据
                    String str = line.replace(oldStr, newStr);
                    //文件节点移动到文件开始
                    System.out.println(str);
                    raf.seek(lastPoint);
                    raf.writeBytes(str);
                }
                lastPoint = point;
            }
        } finally {
            try {
                if (raf != null) raf.close();
            } catch (IOException e) {
                raf = null;
            }
        }
    }

    public static String lastLineOfFile(String filePath) throws IOException {
        RandomAccessFile accessFile = new RandomAccessFile(filePath, "r");
        if (accessFile.length() < 1) return null;
        byte[] bytes = new byte[1];
        long pos = accessFile.length();
        if (pos > 2) {
            accessFile.seek(--pos);
            accessFile.read(bytes);
            String s = new String(bytes);
            accessFile.seek(--pos);
            while (pos > 0 && accessFile.read(bytes) != -1 && "\n".equals(s)) {
                accessFile.seek(--pos);
                s = new String(bytes);
            }
            while (pos > 0 && accessFile.read(bytes) != -1 && !"\n".equals(s)) {
                accessFile.seek(--pos);
                s = new String(bytes);
            }
            if (pos > 0) accessFile.seek(pos + 2);
        }
        return new String(accessFile.readLine().getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * 推测 file 文本文件的行平均长度
     * @param file 输入的文本文件
     * @return 推测的行平均长度
     * @throws IOException 非 text 文件抛出异常
     */
    public static int predictLineSize(File file) throws IOException {
        String type = FileUtils.contentType(file);
        if (!type.startsWith("text") && !type.equals("application/octet-stream")) {
            throw new IOException(file + " may be not a text file");
        }
        BufferedReader reader = new BufferedReader(new FileReader(file));
        int times = 5;
        int size = 0;
        String line;
        while ((line = reader.readLine()) != null && times > 0) {
            times--;
            size += line.length();
        }
        reader.close();
        return size / (5 - times);
    }
}