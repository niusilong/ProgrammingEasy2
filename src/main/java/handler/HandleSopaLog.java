package handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HandleSopaLog {
	public static void main(String[] args) throws IOException {
		String operation = "01";
		if("01".equals(operation)) {    //去掉重复冗余的日志
			String dir = "/home/niusilong/Sopa/src/sopa";
			File originalfile = new File(dir + File.separator+"run_log.sopa");
			originalfile.renameTo(new File(dir + File.separator+"run_log_original.sopa"));
			SopaLogs sopaLogs = new SopaLogs(dir + File.separator+"run_log_original.sopa");
			sopaLogs.checkAndRemove();
			sopaLogs.exportLogFile(dir + File.separator+"run_log.sopa");
//			File originalfile = new File(dir + File.separator+"run_log.sopa");
//			File trimedfile = new File(dir + File.separator+"run_log_trimed.sopa");
//			originalfile.renameTo(new File(dir + File.separator+"run_log_original.sopa"));
//			trimedfile.renameTo(new File(dir + File.separator+"run_log.sopa"));
		}
	}
}
class SopaLogs{
	String logRegEx = "^\\d{4}-[\\d]{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}:\\d{3} [\\s\\S]+->[\\s\\S]+:[\\s\\S]+(-begin|-end){1}$";
	String logPath = "";
	List<String> logs;
	int checkIndex = 0;
	int cursorIndex = 0;
	List<String> checkedMethod = new ArrayList<String>();
	List<Integer> beginIndexs = new ArrayList<Integer>();
	List<Integer> endIndexs = new ArrayList<Integer>();
	String checkClassAndMethod = "";
	int repetitionCount = 0;
	static int REMOVE_COUNT = 10;    //方法循环数超过此数则删除
	static int MAX_BEGIN_END_SPAN = 1;    //低于此跨度则删除
	static int MAX_REMOVE_METHOD_COUNT = 20000;   //最多移除的方法
	int removeMethodCount = 0;
	int loopCount = 0;
	public SopaLogs(String logPath) throws IOException {
		this.logPath = logPath;
		logs = Files.readAllLines(Paths.get(logPath), Charset.forName("UTF-8"));
		checkClassAndMethod = getClassAndMethod(logs.get(0));
	}
	public void checkAndRemove() {
		while(checkIndex < logs.size()){
//			System.out.println("checkIndex: "+checkIndex);
			checkClassAndMethod = getClassAndMethod(logs.get(checkIndex));
//			System.out.println("检查："+checkClassAndMethod);
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
		System.out.println(String.format("第%s轮共删除方法%d条", loopCount, removeMethodCount));
		if(removeMethodCount > 0) {
			beginIndexs.clear();
			endIndexs.clear();
			checkIndex = 0;
			cursorIndex = checkIndex;
			removeMethodCount = 0;
			loopCount++;
			checkAndRemove();
		}
	}
	public boolean innerCheckAndRemove() {
//		System.out.println("cursorIndex: "+cursorIndex);
		String log = "";
		while(cursorIndex < logs.size()) {
//			System.out.println("cursorIndex: "+cursorIndex);
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
			if(endIndexs.size() == 0 && cursorIndex - checkIndex > 4) {
				break;
			}
			if(endIndexs.size() > 1 && endIndexs.get(0)-beginIndexs.get(0) > MAX_BEGIN_END_SPAN) {
				break;
			}
			cursorIndex++;
		}
		if(beginIndexs.size() >= REMOVE_COUNT) {
			List<Integer> toremoveIndexList = beginIndexs;
			toremoveIndexList.addAll(endIndexs);
			Collections.sort(toremoveIndexList);
//			System.out.println(toremoveIndexList);
			for(int i = toremoveIndexList.size()-1; i >= 0; i--) {
				logs.remove(toremoveIndexList.get(i).intValue());
			}
			return true;
		}else {
			return false;
		}
		
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