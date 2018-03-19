package com.test.file;

import java.io.File;

public class HandleFile {
	public static void main(String[] args) {
		String operation = "01";
		if("01".equals(operation)) {
			File dir = new File("/home/niusilong/projects/tf_test/tf_src_rewrited");
			int count = countFile(dir, ".py");
			System.out.println("文件总数为："+count);
		}
	}
	public static int countFile(File dir, String suffix) {
		int count = 0;
		File[] files = dir.listFiles();
		for(File f : files) {
			if(f.isDirectory()) {
				count += countFile(f, suffix);
			}else if(f.getName().endsWith(".py")) {
				count++;
			}
		}
		return count;
	}
}

