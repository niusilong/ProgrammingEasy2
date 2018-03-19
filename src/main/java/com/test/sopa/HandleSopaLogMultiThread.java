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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.test.sopa.thread.ThreadPool;

public class HandleSopaLogMultiThread {
	static int MAX_LOOP = 10;   //循环删除此数
	static int REMOVE_COUNT = 10;    //方法循环数超过此数则删除
	static int MAX_BEGIN_END_SPAN = 1;    //低于此跨度则删除
	static int MAX_REMOVE_METHOD_COUNT = 20000;   //最多移除的方法
	
	public static void main(String[] args) throws IOException {
		String dir = "/home/niusilong/Sopa/src/sopa";
		File originalfile = new File(dir + File.separator+"run_log.sopa");
		File renamedOoriginalfile = new File(dir + File.separator+"run_log_original.sopa");
		if(!renamedOoriginalfile.exists()) {
			originalfile.renameTo(renamedOoriginalfile);
		}
		SopaLogs sopaLogs = new SopaLogs(dir + File.separator+"run_log_original.sopa");
		int loopCount = 0;
		int totalLines = sopaLogs.getLogs().size();
		while(true) {
			loopCount++;
			int removeCount = execute(sopaLogs);
			System.out.println(String.format("总计：第%s轮共删除方法%d个, 原行数%d, 共删除行数：%d, 剩余行数%d, 本轮检查方法%d个", 
					loopCount, removeCount, totalLines, totalLines-sopaLogs.getRestLogCount(), sopaLogs.getRestLogCount(), sopaLogs.getCheckedMethodList().size()));
			
			if(removeCount == 0 || loopCount >= MAX_LOOP) {
				break;
			}else {
				sopaLogs.trimLogs();
			}
		}
		//写出日志文件
		sopaLogs.exportLogFile(dir + File.separator+"run_log.sopa");
		
		ThreadPool.threadPoolExecutor.shutdown();
	}
	public static int execute(SopaLogs sopaLogs) throws IOException {
		
		//多线程删除日志
		List<Future<Boolean>> futureList = new ArrayList<Future<Boolean>>();
		List<String> checkedMethodList = new ArrayList<String>();
		sopaLogs.setCheckedMethodList(checkedMethodList);
		for(int i = 0; i < sopaLogs.getLogs().size(); i++) {
			String log = sopaLogs.getLogs().get(i);
			if(log == null) {
				continue;
			}
			String checkClassAndMethod = SopaLogs.getClassAndMethod(sopaLogs.getLogs().get(i));
			if(checkClassAndMethod == null || checkedMethodList.contains(checkClassAndMethod)
//					|| sopaLogs.getRemovedMethodList().contains(checkClassAndMethod)
					) {
				continue;
			}
			checkedMethodList.add(checkClassAndMethod);
			final int checkIndex = i;
			futureList.add(ThreadPool.threadPoolExecutor.submit(new Callable<Boolean>(){
				@Override
				public Boolean call() throws Exception {
					return sopaLogs.checkAndRemove(checkClassAndMethod, checkIndex);
				}
			}));
		}
		int index = 0;
		int removeCount = 0;
		for(Future<Boolean> future : futureList) {
			try {
				boolean removeRslt = future.get();
				if(removeRslt) {
					removeCount++;
					System.out.println("删除方法: "+checkedMethodList.get(index));
				}
				index++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		return removeCount;
	}
	static class SopaLogs{
		static String logRegEx = "^\\d{4}-[\\d]{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}:\\d{3} [\\s\\S]+->[\\s\\S]+:[\\s\\S]+(-begin|-end){1}$";
		String logPath = "";
		List<String> logs;
		int totalLines = 0;
		List<String> checkedMethodList = new ArrayList<String>();
		List<String> removedMethodList = new ArrayList<String>();
		int repetitionCount = 0;
		int removeMethodCount = 0;
		int loopCount = 0;
		
		public SopaLogs(String logPath) throws IOException {
			this.logPath = logPath;
			logs = Files.readAllLines(Paths.get(logPath), Charset.forName("UTF-8"));
			totalLines = logs.size();
		}
		public void trimLogs() {
			for(int i = 0; i < logs.size(); ) {
				if(logs.get(i) == null) {
					logs.remove(i);
				}else {
					i++;
				}
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
		public boolean checkAndRemove(String checkClassAndMethod, int checkIndex) {
			int cursorIndex = checkIndex;
			List<Integer> beginIndexs = new ArrayList<Integer>();
			List<Integer> endIndexs = new ArrayList<Integer>();
//			System.out.println("cursorIndex: "+cursorIndex);
			String log = "";
			while(cursorIndex < logs.size()) {
//				System.out.println("cursorIndex: "+cursorIndex);
				log = logs.get(cursorIndex);
				if(log == null) {
					cursorIndex++;
					continue;
				}
				if(!log.matches(logRegEx)){
					System.out.println("删除第"+(cursorIndex+1)+"行，正则表达式不符");
					logs.set(cursorIndex, null);
					cursorIndex++;
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
//				if(beginIndexs.size() >= 3  && endIndexs.size() >= 3 && getMinSpan(beginIndexs, endIndexs) > MAX_BEGIN_END_SPAN) {
//					break;
//				}
				cursorIndex++;
			}
//			System.out.println("方法总数: "+beginIndexs.size());
			
			if(beginIndexs.size() >= REMOVE_COUNT && getMinSpan(beginIndexs, endIndexs) <= MAX_BEGIN_END_SPAN) {
//				beginIndexs.remove(0);
//				endIndexs.remove(0);
				List<Integer> toremoveIndexList = beginIndexs;
				toremoveIndexList.addAll(endIndexs);
				Collections.sort(toremoveIndexList);
//				System.out.println(toremoveIndexList);
				for(int i = toremoveIndexList.size()-1; i >= 0; i--) {
					logs.set(toremoveIndexList.get(i).intValue(), null);
				}
				if(!removedMethodList.contains(checkClassAndMethod)) {
					removedMethodList.add(checkClassAndMethod);
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
				if(line == null) {
					continue;
				}
				fw.write(line);
				fw.write("\n");
			}
			fw.flush();
			fw.close();
			
		}
		public static String getClassAndMethod(String log) {
			if(!log.matches(logRegEx)) {
				return null;
			}
			return log.substring(38, log.lastIndexOf("-"));
		}
		public int getRestLogCount() {
			int count = 0;
			for(String log : logs) {
				if(log == null) {
					continue;
				}else {
					count++;
				}
			}
			return count;
		}
		public List<String> getLogs() {
			return logs;
		}
		public void setLogs(List<String> logs) {
			this.logs = logs;
		}
		public List<String> getCheckedMethodList() {
			return checkedMethodList;
		}
		public void setCheckedMethodList(List<String> checkedMethodList) {
			this.checkedMethodList = checkedMethodList;
		}
		public List<String> getRemovedMethodList() {
			return removedMethodList;
		}
		public void setRemovedMethodList(List<String> removedMethodList) {
			this.removedMethodList = removedMethodList;
		}
		
	}
}
