package roart.beans.session.control;

import roart.model.ResultItem;

import javax.servlet.http.*;
import java.util.Vector;
import java.util.Enumeration;

import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import java.io.*;

import roart.dir.Traverse;

import roart.queue.ClientQueueElement;

import roart.model.FileLocation;
import roart.model.IndexFiles;
import roart.queue.Queues;
import roart.thread.IndexRunner;
import roart.thread.OtherRunner;
import roart.thread.TikaRunner;
import roart.content.OtherHandler;
import roart.content.ClientHandler;
import roart.thread.ClientRunner;
import roart.thread.DbRunner;

import roart.dao.SearchDao;
import roart.dao.IndexFilesDao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Main {
    private Log log = LogFactory.getLog(this.getClass());

    // called from ui
    // returns list: new file
    public void traverse(String add) throws Exception {
	ClientQueueElement e = new ClientQueueElement(com.vaadin.ui.UI.getCurrent(), "filesystem", add, null, null, null, false, false);
	Queues.clientQueue.add(e);
    }

    public Set<String> traverse(String add, Set<IndexFiles> newset, List<ResultItem> retList) throws Exception {
	Map<String, HashSet<String>> dirset = new HashMap<String, HashSet<String>>();
	Set<String> filesetnew2 = new HashSet<String>();
	Set<String> filesetnew = Traverse.doList(add, newset, filesetnew2, dirset, null, false, false);    
	for (String s : filesetnew2) {
	    retList.add(new ResultItem(s));
	}
	return filesetnew;
    }

    // called from ui
    // returns list: new file
    public void traverse() throws Exception {
	ClientQueueElement e = new ClientQueueElement(com.vaadin.ui.UI.getCurrent(), "filesystem", null, null, null, null, false, false);
	Queues.clientQueue.add(e);
    }

    public Set<String> traverse(Set<IndexFiles> newindexset, List<ResultItem> retList) throws Exception {
	Set<String> filesetnew = new HashSet<String>();
	retList.addAll(filesystem(newindexset, filesetnew, null));
	return filesetnew;
    }

    static String[] dirlist = null;
    static String[] dirlistnot = null;

    public static void parseconfig() {
	System.out.println("config2 parsed");
	//log.info("config2 parsed");
	String dirliststr = roart.util.Prop.getProp().getProperty("dirlist");
	String dirlistnotstr = roart.util.Prop.getProp().getProperty("dirlistnot");
	dirlist = dirliststr.split(",");
	dirlistnot = dirlistnotstr.split(",");
    }

    private List<ResultItem> filesystem(Set<IndexFiles> indexnewset, Set<String> filesetnew, Set<String> newset) {
	List<ResultItem> retList = new ArrayList<ResultItem>();

	Map<Integer, Set<String>> sortlist = new TreeMap<Integer, Set<String>>();
	Map<String, HashSet<String>> dirset = new HashMap<String, HashSet<String>>();
	try {
	    Set<String> fileset = new HashSet<String>();
	    //Set<String> filesetnew = new HashSet<String>();
	    List<IndexFiles> indexes = IndexFilesDao.getAll();
	    log.info("size " + indexes.size());
	    for (IndexFiles index : indexes) {
		for (FileLocation filename : index.getFilelocations()) {
		    //log.info("size2 " + file.getFilename());
		    fileset.add(filename.toString());
		}
	    }
	    indexes.clear();
	    //Set<String> md5set = new HashSet<String>();

	    for (int i = 0; i < dirlist.length; i ++) {
		Set<String> filesetnew2 = Traverse.doList(dirlist[i], indexnewset, newset, dirset, dirlistnot, false, false);
		filesetnew.addAll(filesetnew2);
	    }
	    //roart.model.HibernateUtil.currentSession().flush();
	    //	    for (String filename : dirset.keySet()) {
	    //	log.info("size2 " + filename);
	    //	filesetnew.add(filename);
	    //}
	    log.info("sizenew " + filesetnew.size());
	    //fileset.removeAll(filesetnew);
	    for (String filename : filesetnew) {
		//log.info("size2 " + filename);
		fileset.remove(filename);
	    }
	    log.info("sizeafter " + fileset.size());
	    for (String filename : fileset) {
		log.info("removing " + filename);
		String md5 = IndexFilesDao.getMd5ByFilename(filename);
		IndexFiles file = IndexFilesDao.getByMd5(md5);
		boolean wasThere = file.removeFile(filename);
		if (!wasThere) {
		    log.error("the file " + filename + " was not in the set");
		}
		//roart.model.HibernateUtil.currentSession().delete(file);
	    }
	    //roart.model.HibernateUtil.commit();
	    //log.info("Hibernate commit");
	    //roart.model.HibernateUtil.currentSession().close();
	} catch (Exception e) {
		log.info(e);
		log.error("Exception", e);
	}
	return retList;
    }

    // called from ui
    public void overlapping() {
	ClientQueueElement e = new ClientQueueElement(com.vaadin.ui.UI.getCurrent(), "overlapping", null, null, null, null, false, false);
	Queues.clientQueue.add(e);
    }

    public List<List> overlappingDo() {
	List<ResultItem> retList = new ArrayList<ResultItem>();
	ResultItem ri = new ResultItem();
	ri.add("Number");
	ri.add("String");
	retList.add(ri);

	Set<String> filesetnew = new HashSet<String>();
	Map<Integer, Set<String>> sortlist = new TreeMap<Integer, Set<String>>();
	Map<Integer, Set<String>> sortlist2 = new TreeMap<Integer, Set<String>>();
	Map<String, HashSet<String>> dirset = new HashMap<String, HashSet<String>>();
	Map<String, HashSet<String>> fileset = new HashMap<String, HashSet<String>>();
	try {
	    Set<String> filesetnew2 = Traverse.doList2(dirset, fileset);
	    filesetnew.addAll(filesetnew2);
	} catch (Exception e) {
		log.info(e);
		log.error("Exception", e);
	}

	List<String> keyList = new ArrayList<String>(dirset.keySet());
	for (int i = 0; i < keyList.size(); i++ ) {
	    for (int j = i+1; j < keyList.size(); j++ ) {
		HashSet<String> set1 = (HashSet<String>) dirset.get(keyList.get(i)).clone();
		HashSet<String> set2 = (HashSet<String>) dirset.get(keyList.get(j)).clone();
		HashSet<String> set3 = (HashSet<String>) dirset.get(keyList.get(i)).clone();
		HashSet<String> set4 = (HashSet<String>) dirset.get(keyList.get(j)).clone();
		int size0 = set1.size() + set2.size();
		set1.retainAll(set2);
		set4.retainAll(set3);
		int size = set1.size() + set4.size();
		if (size0 == 0) {
		    size0 = 1000000;
		}
		int ratio = (int) (100*size/size0);
		if (ratio > 50 && size > 4) {
		    set1.addAll(set4);
		    Integer intI = new Integer(ratio);
		    String sizestr = "" + size;
		    sizestr = "      ".substring(sizestr.length()) + sizestr;
		    String str = sizestr + " : " + keyList.get(i) + " : " + keyList.get(j); // + " " + set1;
		    Set<String> strSet = sortlist.get(intI);
		    if (strSet == null) {
			strSet = new TreeSet<String>();
		    }
		    strSet.add(str);
		    sortlist.put(intI, strSet);
		}
	    }
	}
	for (Integer intI : sortlist.keySet()) {
	    for (String str : sortlist.get(intI)) {
		ResultItem ri2 = new ResultItem();
		ri2.add("" + intI.intValue());
		ri2.add(str);
		retList.add(ri2);
	    }
	}
	for (int i = 0; i < keyList.size(); i++ ) {
	    int fileexist = 0;
	    String dirname = keyList.get(i);
	    Set<String> dirs = dirset.get(dirname);
	    int dirsize = dirs.size();
	    for (String md5 : dirs) {
		Set<String> files = fileset.get(md5);
		if (files != null && files.size() >= 2) {
		    fileexist++;
		}
	    }
	    int ratio = (int) (100*fileexist/dirsize);
	    // overlapping?
	    if (ratio > 50 && dirsize > 4) {
		Integer intI = new Integer(ratio);
		String sizestr = "" + dirsize;
		sizestr = "      ".substring(sizestr.length()) + sizestr;
		String str = sizestr + " : " + dirname;
		Set<String> strSet = sortlist2.get(intI);
		if (strSet == null) {
		    strSet = new TreeSet<String>();
		}
		strSet.add(str);
		sortlist2.put(intI, strSet);
	    }
	}
	for (Integer intI : sortlist2.keySet()) {
	    for (String str : sortlist2.get(intI)) {
                ResultItem ri2 = new ResultItem();
                ri2.add("" + intI.intValue());
                ri2.add(str);
		retList.add(ri2);
	    }
	}
	List<List> retlistlist = new ArrayList<List>();
	retlistlist.add(retList);
	return retlistlist;
    }

    // called from ui
    // returns list: indexed file list
    // returns list: tika timeout
    // returns list: not indexed
    // returns list: deleted
    public void index(String suffix) throws Exception {
	ClientQueueElement e = new ClientQueueElement(com.vaadin.ui.UI.getCurrent(), "reindexsuffix", null, suffix, null, null, true, false);
	Queues.clientQueue.add(e);
    }

    // called from ui
    // returns list: indexed file list
    // returns list: tika timeout
    // returns list: file does not exist
    // returns list: not indexed
    public void index(String add, boolean reindex) throws Exception {
	ClientQueueElement e = new ClientQueueElement(com.vaadin.ui.UI.getCurrent(), "index", add, null, null, null, reindex, false);
	Queues.clientQueue.add(e);
    }

    // called from ui
    // returns list: indexed file list
    // returns list: tika timeout
    // returns list: not indexed
    public void reindexdatelower(String date, boolean reindex) throws Exception {
	ClientQueueElement e = new ClientQueueElement(com.vaadin.ui.UI.getCurrent(), "reindexdate", null, null, date, null, reindex, false);
	Queues.clientQueue.add(e);
    }

    public void reindexdatehigher(String date, boolean reindex) throws Exception {
	ClientQueueElement e = new ClientQueueElement(com.vaadin.ui.UI.getCurrent(), "reindexdate", null, null, null, date, reindex, false);
	Queues.clientQueue.add(e);
    }

    public List<List> clientDo(ClientQueueElement el) throws Exception {
	String function = el.function;
	String filename = el.file;
	boolean reindex = el.reindex;
	log.info("function " + function + " " + filename + " " + reindex);

	Set<List> retlistset = new HashSet<List>();
	List<List> retlistlist = new ArrayList<List>();
	List<ResultItem> retList = new ArrayList<ResultItem>();
	retList.add(Traverse.getHeader());
	List<ResultItem> retTikaTimeoutList = new ArrayList<ResultItem>();
	retTikaTimeoutList.add(new ResultItem("Tika timeout"));
	List<ResultItem> retNotList = new ArrayList<ResultItem>();
	retNotList.add(Traverse.getHeaderNot());
	List<ResultItem> retNewFilesList = new ArrayList<ResultItem>();
	retNewFilesList.add(new ResultItem("New file"));
	List<ResultItem> retDeletedList = new ArrayList<ResultItem>();
	retDeletedList.add(new ResultItem("Deleted"));
	List<ResultItem> retNotExistList = new ArrayList<ResultItem>();
	retNotExistList.add(new ResultItem("File does not exist"));

	Set<String> filesetnew = new HashSet<String>();
	Set<IndexFiles> indexnewset = new HashSet<IndexFiles>();

	Set<String> fileset = new HashSet<String>();
	Set<String> md5set = new HashSet<String>();

	List<List> retlisttmp = null;

	// filesystem
	// reindexsuffix
	// index
	// reindexdate
	// filesystemlucenenew

	DbRunner.doupdate = false;
	if (function.equals("filesystem") || function.equals("filesystemlucenenew") || (function.equals("index") && filename != null && !reindex)) {
	    if (filename != null) {
		filesetnew = traverse(filename, indexnewset, retNewFilesList);
	    } else {
		filesetnew = traverse(indexnewset, retNewFilesList);
	    }
	    if (function.equals("filesystem")) {
		IndexFilesDao.commit();
		retlistlist.add(retNewFilesList);
		DbRunner.doupdate = true;
		return  retlistlist;
	    }
	}

	Collection<IndexFiles> indexes = null;
	if (function.equals("filesystemlucenenew")) {
	    indexes = indexnewset;
	} else if (function.equals("index") && filename != null && !reindex) {
	    Set<IndexFiles> indexset = new HashSet<IndexFiles>();
	    for (String name : filesetnew) {
		String md5 = IndexFilesDao.getMd5ByFilename(name);
		IndexFiles index = IndexFilesDao.getByMd5(md5);
		indexset.add(index);
	    }
	    indexes = indexset;
	} else {
	    indexes = IndexFilesDao.getAll();
	}
	DbRunner.doupdate = true;

	String maxfailedStr = roart.util.Prop.getProp().getProperty("failedlimit");
	int maxfailed = new Integer(maxfailedStr).intValue();

	String maxStr = roart.util.Prop.getProp().getProperty("reindexlimit");
	int max = new Integer(maxStr).intValue();

	Set<IndexFiles> toindexset = new HashSet<IndexFiles>();

	int i = 0;
	for (IndexFiles index : indexes) {

	    // skip if indexed already, and no reindex wanted
	    Boolean indexed = index.getIndexed();
	    if (indexed != null) {
		if (!reindex && indexed.booleanValue()) {
		    continue;
		}
	    }
	    
	    String md5 = index.getMd5();

	    if (maxfailed > index.getFailed().intValue()) {
		continue;
	    }
	    
	    if (function.equals("reindexdate")) {
		i += Traverse.reindexdateFilter(el, index, toindexset, fileset, md5set);
	    }
	    if (function.equals("reindexsuffix")) {
		i += Traverse.reindexsuffixFilter(el, index, el.suffix, toindexset, fileset, md5set);
	    }
	    if (function.equals("index") || function.equals("filesystemlucenenew")) {
		i += Traverse.indexnoFilter(el, index, reindex, toindexset, fileset, md5set);
	    }
	    
	    if (reindex && max > 0 && i > max) {
		break;
	    }
	    
	}

	Map<String, String> filesMapMd5 = new HashMap<String, String>();
	Map<String, Boolean> indexMap = new HashMap<String, Boolean>();
	for (IndexFiles index : toindexset) {
	    String md5 = index.getMd5();
	    String name = Traverse.getExistingFile(index);
	    filesMapMd5.put(md5, name);
	    indexMap.put(md5, index.getIndexed());
	}

	for (String md5 : filesMapMd5.keySet()) {
	    Traverse.indexsingle(retList, retNotList, md5, indexMap, filesMapMd5, reindex, 0);
	}

	while ((Queues.queueSize() + Queues.runSize()) > 0) {
		TimeUnit.SECONDS.sleep(60);
		Queues.queueStat();
	}
	for (String ret : Queues.tikaTimeoutQueue) {
	    retTikaTimeoutList.add(new ResultItem(ret));
	}

	Queues.resetTikaTimeoutQueue();
	IndexFilesDao.commit();

	retlistlist.add(retList);
	retlistlist.add(retNotList);
	retlistlist.add(retNewFilesList);
	retlistlist.add(retDeletedList);
	retlistlist.add(retTikaTimeoutList);
	retlistlist.add(retNotExistList);
	return retlistlist;
    }

    // outdated, did run once, had a bug which made duplicates
    public List<String> cleanup() {
	List<String> retlist = new ArrayList<String>();
	try {
	    return roart.jpa.SearchLucene.removeDuplicate();
	} catch (Exception e) {
		log.info(e);
		log.error("Exception", e);
	}
	return retlist;
    }

    // outdated, used once, when bug added filename instead of md5
    public List<String> cleanup2() {
	List<String> retlist = new ArrayList<String>();
	try {
	    //return roart.jpa.SearchLucene.cleanup2();
	} catch (Exception e) {
		log.info(e);
		log.error("Exception", e);
	}
	return retlist;
    }

    // old, probably oudated by overlapping?
    public List<String> cleanupfs(String dirname) {
	//List<String> retlist = new ArrayList<String>();
	Set<String> filesetnew = new HashSet<String>();
	try {
	    String[] dirlist = { dirname };
	    for (int i = 0; i < dirlist.length; i ++) {
		Set<String> filesetnew2 = Traverse.dupdir(dirlist[i]);
		filesetnew.addAll(filesetnew2);
	    }
	} catch (Exception e) {
		log.info(e);
		log.error("Exception", e);
	}
	return new ArrayList<String>(filesetnew);
    }

    // called from ui
    public void memoryusage() {
	ClientQueueElement e = new ClientQueueElement(com.vaadin.ui.UI.getCurrent(), "memoryusage", null, null, null, null, false, false);
	Queues.clientQueue.add(e);
    }

    public List<List> memoryusageDo() {
	List<ResultItem> retlist = new ArrayList<ResultItem>();
	try {
	    Runtime runtime = Runtime.getRuntime();
	    long maxMemory = runtime.maxMemory();
	    long allocatedMemory = runtime.totalMemory();
	    long freeMemory = runtime.freeMemory();
	    java.text.NumberFormat format = java.text.NumberFormat.getInstance();
	    retlist.add(new ResultItem("free memory: " + format.format(freeMemory / 1024)));
	    retlist.add(new ResultItem("allocated memory: " + format.format(allocatedMemory / 1024)));
	    retlist.add(new ResultItem("max memory: " + format.format(maxMemory / 1024)));
	    retlist.add(new ResultItem("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024)));
	} catch (Exception e) {
		log.info(e);
		log.error("Exception", e);
	}
	List<List> retlistlist = new ArrayList<List>();
	retlistlist.add(retlist);
	return retlistlist;
    }

    // called from ui
    // returns list: not indexed
    // returns list: another with columns
    public void notindexed() throws Exception {
	ClientQueueElement e = new ClientQueueElement(com.vaadin.ui.UI.getCurrent(), "notindexed", null, null, null, null, false, false);
	Queues.clientQueue.add(e);
    }

    public List<List> notindexedDo() throws Exception {
	List<List> retlistlist = new ArrayList<List>();
	List<ResultItem> retlist = new ArrayList<ResultItem>();
	List<ResultItem> retlist2 = new ArrayList<ResultItem>();
	ResultItem ri3 = new ResultItem();
	ri3.add("Column 1");
	ri3.add("Column 2");
	ri3.add("Column 3");
	retlist2.add(ri3);
	List<ResultItem> retlistyes = null;
	try {
	    retlist.addAll(Traverse.notindexed());
	    retlistyes = Traverse.indexed();
	    Map<String, Integer> plusretlist = new HashMap<String, Integer>();
	    Map<String, Integer> plusretlistyes = new HashMap<String, Integer>();
	    for(ResultItem ri : retlist) {
		if (ri == retlist.get(0)) {
		    continue;
		}
		String filename = ri.get().get(10);
		if (filename == null) {
		    continue;
		}
		int ind = filename.lastIndexOf(".");
		if (ind == -1 || ind <= filename.length() - 6) {
		    continue;
		}
		String suffix = filename.substring(ind+1);
		Integer i = plusretlist.get(suffix);
		if (i == null) {
		    i = new Integer(0);
		}
		i++;
		plusretlist.put(suffix, i);
	    }
	    for(ResultItem ri : retlistyes) {
		String filename = ri.get().get(0); // or for a whole list?
		if (filename == null) {
		    continue;
		}
		int ind = filename.lastIndexOf(".");
		if (ind == -1 || ind <= filename.length() - 6) {
		    continue;
		}
		String suffix = filename.substring(ind+1);
		Integer i = plusretlistyes.get(suffix);
		if (i == null) {
		    i = new Integer(0);
		}
		i++;
		plusretlistyes.put(suffix, i);
	    }
	    log.info("size " + plusretlist.size());
	    log.info("sizeyes " + plusretlistyes.size());
	    for(String string : plusretlist.keySet()) {
		ResultItem ri2 = new ResultItem();
		ri2.add("Format");
		ri2.add(string);
		ri2.add("" + plusretlist.get(string).intValue());
		retlist2.add(ri2);
	    }
	    for(String string : plusretlistyes.keySet()) {
		ResultItem ri2 = new ResultItem();
		ri2.add("Formatyes");
		ri2.add(string);
		ri2.add("" + plusretlistyes.get(string).intValue());
		retlist2.add(ri2);
	    }
	} catch (Exception e) {
	    log.info(e);
	    log.error("Exception", e);
	}
	log.info("sizes " + retlist.size() + " " + retlist2.size() + " " + System.currentTimeMillis());
	retlistlist.add(retlist);
	retlistlist.add(retlist2);
	return retlistlist;
    }

    // called from ui
    // returns list: indexed file list
    // returns list: tika timeout
    // returns list: new file
    // returns list: file does not exist
    // returns list: not indexed
    public void filesystemlucenenew() throws Exception {
	ClientQueueElement e = new ClientQueueElement(com.vaadin.ui.UI.getCurrent(), "filesystemlucenenew", null, null, null, null, false, false);
	Queues.clientQueue.add(e);
    }

    public void filesystemlucenenew(String add, boolean md5checknew) throws Exception {
	ClientQueueElement e = new ClientQueueElement(com.vaadin.ui.UI.getCurrent(), "filesystemlucenenew", add, null, null, null, false, md5checknew);
	Queues.clientQueue.add(e);
    }

    private static TikaRunner tikaRunnable = null;
    private static Thread tikaWorker = null;
    private static IndexRunner indexRunnable = null;
    private static Thread indexWorker = null;
    private static OtherRunner otherRunnable = null;
    private static Thread otherWorker = null;
    private static ClientRunner clientRunnable = null;
    private static Thread clientWorker = null;
    private static DbRunner dbRunnable = null;
    private static Thread dbWorker = null;

    public void startThreads() {
    	if (tikaRunnable == null) {
	    String timeoutstr = roart.util.Prop.getProp().getProperty("tikatimeout");
	    int timeout = new Integer(timeoutstr).intValue();
	    TikaRunner.timeout = timeout;

    	tikaRunnable = new TikaRunner();
    	tikaWorker = new Thread(tikaRunnable);
    	tikaWorker.setName("TikaWorker");
    	tikaWorker.start();
    	}
    	if (indexRunnable == null) {
    	indexRunnable = new IndexRunner();
    	indexWorker = new Thread(indexRunnable);
    	indexWorker.setName("IndexWorker");
    	indexWorker.start();
    	}
    	if (otherRunnable == null) {
	    String timeoutstr = roart.util.Prop.getProp().getProperty("othertimeout");
	    int timeout = new Integer(timeoutstr).intValue();
	    OtherHandler.timeout = timeout;

    	otherRunnable = new OtherRunner();
    	otherWorker = new Thread(otherRunnable);
    	otherWorker.setName("OtherWorker");
    	otherWorker.start();
    	}
    	if (clientRunnable == null) {
    	clientRunnable = new ClientRunner();
    	clientWorker = new Thread(clientRunnable);
    	clientWorker.setName("ClientWorker");
    	clientWorker.start();
    	}
    	if (dbRunnable == null) {
    	dbRunnable = new DbRunner();
    	dbWorker = new Thread(dbRunnable);
    	dbWorker.setName("DbWorker");
    	dbWorker.start();
    	}
    }

    private List<List> mergeListSet(Set<List> listSet, int size) {
	List<List> retlistlist = new ArrayList<List>();
	for (int i = 0 ; i < size ; i++ ) {
	    List<ResultItem> retlist = new ArrayList<ResultItem>();
	    retlistlist.add(retlist);
	}
	for (List<List> listArray : listSet) {
	    for (int i = 0 ; i < size ; i++ ) {
		retlistlist.get(i).addAll(listArray.get(i));
	    }
	}
	return retlistlist;
    }
    
}
