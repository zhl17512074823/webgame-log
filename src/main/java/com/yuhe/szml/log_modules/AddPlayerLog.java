package com.yuhe.szml.log_modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.yuhe.szml.db.log.CommonDB;
import com.yuhe.szml.db.statics.AddPlayerDB;
import com.yuhe.szml.utils.DateUtils2;
import com.yuhe.szml.utils.RegUtils;

import net.sf.json.JSONObject;

public class AddPlayerLog extends AbstractLogModule {

	private static final String[] LOG_COLS = { "Uid", "Urs", "Sex" };
	private static final String[] DB_COLS = { "HostID", "Uid", "Urs", "Sex", "Time" };
	private static String TBL_NAME = "tblAddPlayerLog";
	@Override
	public Map<String, List<Map<String, String>>> execute(List<String> logList, Map<String, String> hostMap) {
		Map<String, List<Map<String, String>>> platformResults = new HashMap<String, List<Map<String, String>>>();
		Map<String, Map<String, Map<String, Integer>>> platformNums = new HashMap<String, Map<String, Map<String, Integer>>>();
		for (String logStr : logList) {
			JSONObject json = JSONObject.fromObject(logStr);
			if (json != null) {
				String message = json.getString("message");
				String hostID = json.getString("hostid");
				if (!message.isEmpty() && !message.equals(" ") && hostMap.containsKey(hostID)) {
					Map<String, String> map = new HashMap<String, String>();
					map.put("HostID", hostID);
					String time = RegUtils.getLogTime(message);
					map.put("Time", time);
					for (String col : LOG_COLS) {
						String value = RegUtils.getLogValue(message, col, "");
						map.put(col, value);
					}
					String platformID = hostMap.get(hostID);
					List<Map<String, String>> platformResult = platformResults.get(platformID);
					if (platformResult == null)
						platformResult = new ArrayList<Map<String, String>>();
					platformResult.add(map);
					platformResults.put(platformID, platformResult);
					//同时还要记录该服的总人数
					Map<String, Map<String, Integer>> platformNum = platformNums.getOrDefault(platformID, new HashMap<String, Map<String, Integer>>());
					Map<String, Integer> hostNums = platformNum.getOrDefault(hostID, new HashMap<String, Integer>());
					if(map.get("Sex").equals("1")){
						hostNums.put("Male", hostNums.getOrDefault("Male", 0) + 1);
					}else{
						hostNums.put("Female", hostNums.getOrDefault("Female", 0) + 1);
					}
					hostNums.put("TotalNum", hostNums.getOrDefault("TotalNum", 0) + 1);
					hostNums.put("Time", DateUtils2.GetTimestamp(time));
					platformNum.put(hostID, hostNums);
					platformNums.put(platformID, platformNum);
				}
			}
		}
		// 插入数据库
		Iterator<String> it = platformResults.keySet().iterator();
		while (it.hasNext()) {
			String platformID = it.next();
			List<Map<String, String>> platformResult = platformResults.get(platformID);
			CommonDB.batchInsertByDate(platformID, platformResult,DB_COLS, TBL_NAME);
			//记录5分钟实时注册人数
			Map<String, Map<String,Integer>> platformNum = platformNums.get(platformID);
			
			AddPlayerDB.batchInsert(platformID, platformNum);
		}
		return platformResults;
	}
	@Override
	public String getStaticsIndex() {
		return "HistoryReg";
	}

}
