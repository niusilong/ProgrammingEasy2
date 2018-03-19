package com.test.sopa;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class HandleSopaLogSingleThread {
	public static void main(String[] args) throws IOException {
		String operation = "01";
		if("01".equals(operation)) {    //去掉重复冗余的日志
			String dir = "/home/niusilong/Sopa/src/sopa";
			File originalfile = new File(dir + File.separator+"run_log.sopa");
			File renamedOoriginalfile = new File(dir + File.separator+"run_log_original.sopa");
			if(!renamedOoriginalfile.exists()) {
				originalfile.renameTo(renamedOoriginalfile);
			}
			SopaLogs sopaLogs = new SopaLogs(dir + File.separator+"run_log_original.sopa");
			sopaLogs.checkAndRemove();
			sopaLogs.exportLogFile(dir + File.separator+"run_log.sopa");
//			File originalfile = new File(dir + File.separator+"run_log.sopa");
//			File trimedfile = new File(dir + File.separator+"run_log_trimed.sopa");
//			originalfile.renameTo(new File(dir + File.separator+"run_log_original.sopa"));
//			trimedfile.renameTo(new File(dir + File.separator+"run_log.sopa"));
		}
	}
	/*
	public static void execute(SopaLogs checkIndex) {
		List<Future<List<String>>> futureList = new ArrayList<Future<List<String>>>();
		int startIndex = 0;
		while(startIndex < dataList.size()){
			int endIndex = (startIndex+LINES_COUNT_PER_THREAD) > dataList.size() ? dataList.size() : (startIndex+LINES_COUNT_PER_THREAD);
			final List<LineData> lineDatas = dataList.subList(startIndex, endIndex);
			startIndex = endIndex;
			futureList.add(ThreadPool.threadPoolExecutor.submit(new Callable<List<String>>(){
	}
	*/
	static class SopaLogs{
		List<String> checkedList = new ArrayList<String>();
		String logRegEx = "^\\d{4}-[\\d]{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}:\\d{3} [\\s\\S]+->[\\s\\S]+:[\\s\\S]+(-begin|-end){1}$";
		String logPath = "";
		List<String> logs;
		int totalLines = 0;
		int checkIndex = 0;
		int cursorIndex = 0;
		List<String> checkedMethod = new ArrayList<String>();
		List<Integer> beginIndexs = new ArrayList<Integer>();
		List<Integer> endIndexs = new ArrayList<Integer>();
		String checkClassAndMethod = "";
		int repetitionCount = 0;
		static int REMOVE_COUNT = 20;    //方法循环数超过此数则删除
		static int MAX_BEGIN_END_SPAN = 1;    //低于此跨度则删除
		static int MAX_REMOVE_METHOD_COUNT = 20000;   //最多移除的方法
		int removeMethodCount = 0;
		int loopCount = 0;
		static int MAX_LOOP_COUNT = 10;
		public SopaLogs(String logPath) throws IOException {
			this.logPath = logPath;
			logs = Files.readAllLines(Paths.get(logPath), Charset.forName("UTF-8"));
			totalLines = logs.size();
			checkClassAndMethod = getClassAndMethod(logs.get(0));
		}
		public void checkAndRemove() {
			loopCount++;
			while(checkIndex < logs.size()){
				if(checkIndex % 1000 == 0) {
					System.out.println("checkIndex: "+checkIndex);
				}
				checkClassAndMethod = getClassAndMethod(logs.get(checkIndex));
				if(containsChecked(checkedList, checkClassAndMethod)) {
					checkIndex++;
					cursorIndex = checkIndex;
					continue;
				}else {
					checkedList.add(checkClassAndMethod);
				}
//				System.out.println("检查："+checkClassAndMethod);
				boolean removed = innerCheckAndRemove();
				if(removed) {
					removeMethodCount++;
					if(removeMethodCount >= MAX_REMOVE_METHOD_COUNT) {
						break;
					}
					System.out.println("删除："+checkClassAndMethod);
				}
				beginIndexs.clear();
				endIndexs.clear();
				checkIndex++;
				cursorIndex = checkIndex;
			}
			System.out.println(String.format("总计：第%s轮共删除方法%d个, 原行数%d, 删除行数：%d, 剩余行数%d, 本轮检查方法%d个", loopCount, removeMethodCount, totalLines, totalLines-logs.size(), logs.size(), checkedList.size()));
			if(removeMethodCount > 0 && loopCount < MAX_LOOP_COUNT) {
				beginIndexs.clear();
				endIndexs.clear();
				checkIndex = 0;
				cursorIndex = checkIndex;
				removeMethodCount = 0;
				checkedList.clear();
				checkAndRemove();
			}
		}
		public boolean containsChecked(List<String> checkedList, String method) {
			for(int i = checkedList.size()-1; i >=0; i--) {
				if(checkedList.get(i).equals(method)) {
					return true;
				}
			}
			return false;
			
		}
		public boolean innerCheckAndRemove() {
//			System.out.println("cursorIndex: "+cursorIndex);
			String log = "";
			while(cursorIndex < logs.size()) {
//				System.out.println("cursorIndex: "+cursorIndex);
				log = logs.get(cursorIndex);
				if(!log.matches(logRegEx)){
					System.out.println("删除第"+(cursorIndex+1)+"行，正则表达式不符");
					logs.remove(cursorIndex);
					continue;
				}
				if(getClassAndMethod(log).equals(checkClassAndMethod)) {
					if("begin".equals(log.substring(log.lastIndexOf("-")+1))) {
						if(beginIndexs.size() == endIndexs.size()) {   //正常
							beginIndexs.add(cursorIndex);
						}else {
							cursorIndex++;
							continue;
						}
					}else if("end".equals(log.substring(log.lastIndexOf("-")+1))) {
						if(beginIndexs.size()-1 == endIndexs.size()) {   //正常
							endIndexs.add(cursorIndex);
						}else if(beginIndexs.size() == endIndexs.size()) {
							cursorIndex++;
							continue;
						}
					}
				}
//				if(endIndexs.size() == 0 && cursorIndex - checkIndex > 4) {   //第一次检查为中间包含多个方法，则直接跳过
//					break;
//				}
				if(beginIndexs.size() >= 3  && endIndexs.size() >= 3 && getMinSpan(beginIndexs, endIndexs) > MAX_BEGIN_END_SPAN) {
					break;
				}
				cursorIndex++;
			}
//			System.out.println("方法总数: "+beginIndexs.size());
			
			if(beginIndexs.size() >= REMOVE_COUNT) {
				List<Integer> toremoveIndexList = beginIndexs;
				toremoveIndexList.addAll(endIndexs);
				Collections.sort(toremoveIndexList);
//				System.out.println(toremoveIndexList);
				for(int i = toremoveIndexList.size()-1; i >= 0; i--) {
					logs.remove(toremoveIndexList.get(i).intValue());
				}
				return true;
			}else {
				return false;
			}
			
		}
		private int getMinSpan(List<Integer> beginIndexs, List<Integer> endIndexs) {
//			System.out.println("beginIndexs: "+beginIndexs);
//			System.out.println("endIndexs: "+endIndexs);
			int minSpan = MAX_BEGIN_END_SPAN+1;
			for(int i = 0; i < beginIndexs.size();) {
				if(i < beginIndexs.size() && i < endIndexs.size()) {
					int span = endIndexs.get(i)-beginIndexs.get(i);
					if(span > MAX_BEGIN_END_SPAN) {
						beginIndexs.remove(i);
						endIndexs.remove(i);
						continue;
					}
					if(span < minSpan) {
						minSpan = span;
					}
				}
				i++;
			}
//			System.out.println("minSpan="+minSpan);
			return minSpan;
		}
		public void exportLogFile(String path) throws IOException {
			FileWriter fw = new FileWriter(path);
			for(String line : logs) {
				fw.write(line);
				fw.write("\n");
			}
			fw.flush();
			fw.close();
			
		}
		private String getClassAndMethod(String log) {
			return log.substring(38, log.lastIndexOf("-"));
		}
	}
}
