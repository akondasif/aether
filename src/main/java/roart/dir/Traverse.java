package roart.dir;

import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import roart.queue.Queues;
import roart.queue.TikaQueueElement;
import roart.queue.ClientQueueElement;
import roart.queue.ClientQueueElement.Function;
import roart.queue.TraverseQueueElement;
import roart.service.ControlService;
import roart.service.SearchService;
import roart.thread.ClientRunner;
import roart.thread.TikaRunner;
import roart.database.IndexFilesDao;
import roart.filesystem.FileSystemDao;
import roart.model.FileObject;
import roart.model.IndexFiles;
import roart.model.FileLocation;
import roart.model.ResultItem;
import roart.model.SearchDisplay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import roart.util.ConfigConstants;
import roart.util.Constants;
import roart.util.ExecCommand;
import roart.util.MyAtomicLong;
import roart.util.MyAtomicLongs;
import roart.util.MyLock;
import roart.util.MyLockFactory;
import roart.util.MyQueue;
import roart.util.MyQueues;
import roart.util.MySet;
import roart.util.MySets;

import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.tika.metadata.Metadata;

import com.vaadin.ui.UI;


public class Traverse {

    private static final int MAXFILE = 500;

    private static Logger log = LoggerFactory.getLogger(Traverse.class);

    private static boolean indirlistnot(String filename, String[] dirlistnot) {
	if (dirlistnot == null) {
	    return false;
	}
	for (int i = 0; i < dirlistnot.length; i++) {
	    if (filename.indexOf(dirlistnot[i])>=0) {
		return true;
	    }
	}
	return false;
    }

    // cases:
    // INDEX
    // reindex true, path null, not used
    // reindex true, path non-null, reindex given path
    // reindex false, path null, for indexing all unindexed found in all fs (eventually from db)
    // reindex false, path non-null, for indexing all unindexed found in given path 
    // REINDEXLANGUAGE
    // reindex true
    // path null
    // on lang
    // (eventually from db) 
    // REINDEXSUFFIX
    // reindex true or false
    // path null
    // on suffix
    // (eventually from db) 
    // REINDEXDATE
    // reindex true
    // path null
    // on date
    // (eventually from db) 
    // FILESYSTEM
    // check all fs for new if null path
    // check gives fs for new with path
    // FILESYSTEMLUCENENEW
    // check all fs for new if null path
    // check gives fs for new with path, eventually also compute new md5
    // then index if new
    
    public Set<String> doList(String dirname) throws Exception {
        Set<String> retset = new HashSet<String>();
        int max = ControlService.configMap.get(ControlService.Config.REINDEXLIMIT);
        MyAtomicLong indexcount = MyAtomicLongs.get(Constants.INDEXCOUNT + myid); 
        if (element.reindex && max > 0 && indexcount.get() > max) {
            return retset;
        }

	int maxindex = ControlService.configMap.get(ControlService.Config.INDEXLIMIT);
        if (!element.reindex && maxindex > 0 && indexcount.get() > maxindex) {
            return retset;
        }

        if (indirlistnot(dirname, dirlistnot)) {
            return retset;
        }
        //HashSet<String> md5set = new HashSet<String>();
        FileObject dir = FileSystemDao.get(dirname);
        List<FileObject> listDir = FileSystemDao.listFiles(dir);
        //log.info("dir " + dirname);
        //log.info("listDir " + listDir.length);
        if (listDir == null) {
            return retset;
        }
        for (FileObject fo : listDir) {
            String filename = FileSystemDao.getAbsolutePath(fo);
	    // for encoding problems
	    if (!FileSystemDao.exists(fo)) {
		MySet<String> notfoundset = (MySet<String>) MySets.get(notfoundsetid); 
		notfoundset.add(filename);
		continue;
		//throw new FileNotFoundException("File does not exist " + filename);
	    }
            if (filename.length() > MAXFILE) {
                log.info("Too large filesize " + filename);
                continue;
            }
            //log.info("file " + filename);
            if (FileSystemDao.isDirectory(fo)) {
                log.debug("isdir " + filename);
                retset.addAll(doList(filename));
            } else {
                retset.add(filename);
                if (!nomd5) {
                    String queueid = Constants.TRAVERSEQUEUE;
                    /*
                    MyQueue<FileObject> queue2 = MyQueues.get(queueid+"2");
                    try {
                        queue2.offer(fo);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        XMLEncoder encoder = new XMLEncoder( bos );
                        encoder.writeObject( fo );
                        encoder.close();
                        log.info(encoder.toString());
                    } catch (Exception e) {
                        log.error(Constants.EXCEPTION, e);
                    }
                    MyQueue<ClientQueueElement> queue3 = MyQueues.get(queueid+"3");
                    try {
                        queue3.offer(new ClientQueueElement(null, null, queueid, queueid, queueid, queueid, nomd5, nomd5, nomd5));
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        XMLEncoder encoder = new XMLEncoder( bos );
                        encoder.writeObject( new ClientQueueElement(null, null, queueid, queueid, queueid, queueid, nomd5, nomd5, nomd5) );
                        encoder.close();
                        log.info(encoder.toString());
                    } catch (Exception e) {
                        log.error(Constants.EXCEPTION, e);
                    }
                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        XMLEncoder encoder = new XMLEncoder( bos );
                        TraverseQueueElement trav = new TraverseQueueElement(myid, fo, element, retlistid, retnotlistid, newsetid, notfoundsetid);
                        encoder.writeObject( trav);
                        encoder.close();
                        log.info(encoder.toString());
                    } catch (Exception e) {
                        log.error(Constants.EXCEPTION, e);
                    }
                    */
                    MyQueue<TraverseQueueElement> queue = MyQueues.get(queueid);
                    TraverseQueueElement trav = new TraverseQueueElement(myid, filename, element, retlistid, retnotlistid, newsetid, notfoundsetid);
                    queue.offer(trav);

                    //TraverseFile.handleFo3(null, fo);
                }
            }
        }
        /*
        if (dirset != null) {
            dirset.put(dirname, md5set);
        }
        */
        //log.info("retsize " + retset.size());
        return retset;
    }

    // retset will be returned empty
    // dirset will contain a map of directories, and the md5 files is contains
    // fileset will contain a map of md5 and the directories it has files in
    public static Set<String> doList2 (Map<String, HashSet<String>> dirset, Map<String, HashSet<String>> fileset) throws Exception {
	Set<String> retset = new HashSet<String>();

	List<IndexFiles> files = IndexFilesDao.getAll();
	log.info("size " + files.size());
	for (IndexFiles file : files) {
	    String md5 = file.getMd5();
	    for (FileLocation filename : file.getFilelocations()) {
		FileObject tmpfile = FileSystemDao.get(filename.getFilename());
		FileObject dir = FileSystemDao.getParent(tmpfile);
		String dirname = FileSystemDao.getAbsolutePath(dir);
		HashSet<String> md5set = dirset.get(dirname);
		if (md5set == null) {
		    md5set = new HashSet<String>();
		    dirset.put(dirname, md5set);
		}
		md5set.add(md5);

		HashSet<String> dir5set = fileset.get(md5);
		if (dir5set == null) {
		    dir5set = new HashSet<String>();
		    fileset.put(md5, dir5set);
		}
		dir5set.add(dirname);
	    }
	}
	return retset;
    }

    // old, probably oudated by overlapping?
    public static Set<String> dupdir (String dirname) throws Exception {
	boolean onlyone = false;
	boolean error = false;
	int count = 0;
	long size = 0;
	Set<String> retset = new HashSet<String>();
	HashSet<String> md5set = new HashSet<String>();
	FileObject dir = FileSystemDao.get(dirname);
	List<FileObject> listDir = FileSystemDao.listFiles(dir);
	for (FileObject fo : listDir) {
	    String filename = FileSystemDao.getAbsolutePath(fo);
	    if (filename.length() > MAXFILE) {
		log.info("Too large filesize " + filename);
		error = true;
		continue;
	    }
	    if (FileSystemDao.isDirectory(fo)) {
		retset.addAll(dupdir(filename));
	    } else {
		if (error) {
		    continue;
		}
		String md5 = IndexFilesDao.getMd5ByFilename(filename);
		IndexFiles files = IndexFilesDao.getByMd5(md5);
		if (files == null) {
		    error = true;
		    continue;
		}
		if (md5 == null) {
		    error = true;
		    continue;
		}
		if (IndexFilesDao.getByMd5(md5).getFilelocations().size() < 2) {
		    onlyone = true;
		}
		count++;
		//size+=new File(filename).length();
	    }
	}
	if (!error && !onlyone && count>0) {
	    retset.add(dirname + " size " + size);
	}
	return retset;
    }

    public static int indexnoFilter(IndexFiles index, TraverseQueueElement element) throws Exception {
	String md5 = index.getMd5();
	String filename = getExistingLocalFile(index);
    if (filename == null) {
    	log.error("filename should not be null " + md5);
    	return 0;
    }
	if (filename != null) {
	    return 1;
	}
	return 0;
    }

    public static int reindexsuffixFilter(IndexFiles index, TraverseQueueElement element) throws Exception {
	String md5 = index.getMd5();
	for (FileLocation fl : index.getFilelocations()) {
	    if (element.getClientQueueElement().suffix != null && !fl.isLocal() && !fl.getFilename().endsWith(element.getClientQueueElement().suffix)) {
		continue;
	    }
	    FileObject file = FileSystemDao.get(fl.toString());
	    if (!FileSystemDao.exists(file)) {
		continue;
	    }
	    return 1;
	}
	return 0;
    }

    public static int reindexdateFilter(IndexFiles index, TraverseQueueElement element) throws Exception {
	String lowerdate = element.getClientQueueElement().lowerdate;
	String higherdate = element.getClientQueueElement().higherdate;
	Long tslow = null;
	if (lowerdate != null) {
	    tslow = new Long(lowerdate);
	}
	Long tshigh = null;
	if (higherdate != null) {
	    tshigh = new Long(higherdate);
	}

	String timestamp = index.getTimestamp();
	if (timestamp != null) {
	    if (tslow != null && new Long(timestamp).compareTo(tslow) >= 0) {
		return 0;
	    }
	    if (tshigh != null && new Long(timestamp).compareTo(tshigh) <= 0) {
		return 0;
	    }
	} else {
	    return 0;
	}
	String md5 = index.getMd5();
	String filename = getExistingLocalFile(index);

	if (filename == null) {
	    log.error("md5 filename null " + md5);
	    return 0;
	}

	return 1;
    }

    public static List<ResultItem> notindexed(ClientQueueElement el) throws Exception {
	List<ResultItem> retlist = new ArrayList<ResultItem>();
	ResultItem ri = new ResultItem();
	SearchDisplay display = el.display;
	retlist.add(IndexFiles.getHeader(display));
	List<IndexFiles> indexes = IndexFilesDao.getAll();
	log.info("sizes " + indexes.size());
	for (IndexFiles index : indexes) {
	    Boolean indexed = index.getIndexed();
	    if (indexed != null && indexed.booleanValue() == true) {
		continue;
	    }
	    ri = IndexFiles.getResultItem(index, index.getLanguage(), display);
	    retlist.add(ri);
	}
	return retlist;
    }

    public static List<ResultItem> indexed(ClientQueueElement el) throws Exception {
	List<ResultItem> retlist = new ArrayList<ResultItem>();
	List<IndexFiles> indexes = IndexFilesDao.getAll();
	SearchDisplay display = el.display;
	log.info("sizes " + indexes.size());
	for (IndexFiles index : indexes) {
	    Boolean indexed = index.getIndexed();
	    for (FileLocation filename : index.getFilelocations()) {
	    	if (indexed != null) {
	    		if (indexed.booleanValue()) {
	    			retlist.add(IndexFiles.getResultItem(index, index.getLanguage(), display));
	    		}
			}
	    }
	}
	return retlist;
    }

    public static String getExistingLocalFile(IndexFiles i) {
    	FileLocation fl = getExistingLocalFilelocation(i);
    	if (fl != null) {
    		return fl.getFilename();
    	}
    	return null;
    }
    
    public static FileLocation getExistingLocalFilelocation(IndexFiles i) {
	// next up : locations
    	Set<FileLocation> filelocations = i.getFilelocations();
    	if (filelocations == null) {
    		return null;
    	}
    	for (FileLocation filelocation : filelocations) {
    		String node = filelocation.getNode();
    		String filename = filelocation.getFilename();
    		if (node == null || node.equals(ControlService.nodename)) {
    			FileObject file = FileSystemDao.get(filename);
    			if (FileSystemDao.exists(file)) {
    				return filelocation;			
    			}
    		}
    	}
	return null;
    }
    
    public static FileLocation getExistingLocalFilelocationMaybe(IndexFiles i) {
	// next up : locations
    	FileLocation fl = getExistingLocalFilelocation(i);
    	if (fl != null) {
    		return fl;
    	}
    	Set<FileLocation> filelocations = i.getFilelocations();
    	if (filelocations == null || filelocations.size() == 0) {
    		return null;
    	}
    	for (FileLocation filelocation : filelocations) {
    		return filelocation;
	    }
    	return null;
    }

	public static int reindexlanguageFilter(IndexFiles index, TraverseQueueElement element) {
			String mylanguage = index.getLanguage();
			if (mylanguage != null && mylanguage.equals(element.getClientQueueElement().suffix)) { // stupid overload
				String md5 = index.getMd5();
				String filename = getExistingLocalFile(index);

				if (filename == null) {
				    log.error("md5 filename null " + md5);
				    return 0;
				}

			    return 1;
			}
			return 0;
		 	
	}
    
	public static boolean filterindex(IndexFiles index, TraverseQueueElement trav)
			throws Exception {
	    if (index == null) {
	        return false;
	    }
		// skip if indexed already, and no reindex wanted
		Boolean indexed = index.getIndexed();
		if (indexed != null) {
		if (!trav.getClientQueueElement().reindex && indexed.booleanValue()) {
		    return false;
		}
		}
		
		String md5 = index.getMd5();

		// if ordinary indexing (no reindexing)
		// and a failed limit it set
		// and the file has come to that limit

		int maxfailed = ControlService.configMap.get(ControlService.Config.FAILEDLIMIT);
		if (!trav.getClientQueueElement().reindex && maxfailed > 0 && maxfailed <= index.getFailed().intValue()) {
		return false;
		}
		
        MyAtomicLong indexcount = MyAtomicLongs.get(Constants.INDEXCOUNT + trav.getMyid()); 
		
		int indexinc = 0;
		if (trav.getClientQueueElement().function == Function.REINDEXDATE) {
		    indexinc = reindexdateFilter(index, trav);
		    indexcount.addAndGet(indexinc);
		    return indexinc > 0;
		}
		if (trav.getClientQueueElement().function == Function.REINDEXSUFFIX) {
		    indexinc = reindexsuffixFilter(index, trav);
		    indexcount.addAndGet(indexinc);
		    return indexinc > 0;
		}
		if (trav.getClientQueueElement().function == Function.INDEX || trav.getClientQueueElement().function == Function.FILESYSTEMLUCENENEW) {
		    indexinc = indexnoFilter(index, trav);
		    indexcount.addAndGet(indexinc);
		    return indexinc > 0;
		}
		if (trav.getClientQueueElement().function == Function.REINDEXLANGUAGE) {
		    indexinc = reindexlanguageFilter(index, trav);
		    indexcount.addAndGet(indexinc);
		    return indexinc > 0;
		}
		return false;
	}
	
	public Set<String> traversedb() throws Exception {
        int max = ControlService.configMap.get(ControlService.Config.REINDEXLIMIT);
		int maxindex = ControlService.configMap.get(ControlService.Config.INDEXLIMIT);
        MyAtomicLong indexcount = MyAtomicLongs.get(Constants.INDEXCOUNT + myid); 
        String queueid = Constants.TRAVERSEQUEUE;
        MyQueue<TraverseQueueElement> queue = MyQueues.get(queueid);
        List<IndexFiles> indexes = IndexFilesDao.getAll();
		for (IndexFiles index : indexes) {
			if (element.reindex && max > 0 && indexcount.get() > max) {
				break;
			}
		
			if (!element.reindex && maxindex > 0 && indexcount.get() > maxindex) {
				break;
			}
			String md5 = index.getMd5();
			String name = getExistingLocalFile(index);
			if (name == null) {
				log.error("filename should not be null " + md5);
				continue;
			}
			// TODO check if fo needed
            TraverseQueueElement trav = new TraverseQueueElement(myid, name, element, retlistid, retnotlistid, newsetid, notfoundsetid);
			if (!filterindex(index, trav)) {
				continue;
			}
	        // config with finegrained distrib
	        queue.offer(trav);
	        //TraverseFile.indexsingle(trav, md5, name, index);
		}
		
		return null;
	}
	
    public Set<String> traverse(String add) throws Exception {
    	try {
    		log.info("function: " + element.function);
    		if (element.function == Function.REINDEXDATE || element.function == Function.REINDEXLANGUAGE || element.function == Function.REINDEXSUFFIX || (element.function == Function.INDEX && add == null)) {
    			return traversedb();
    		}
    		if (add != null) {
    			return doList(add);    
    		} else {
    			Set<String> retList = new HashSet<String>();
    			for (int i = 0; i < ControlService.dirlist.length; i ++) {
    				retList.addAll(doList(ControlService.dirlist[i]));
    			}
    		}
    	} catch (Exception e) {
    		log.error(Constants.EXCEPTION, e);
    	}
    	return null;
    }

	//int max = 0;
	//int indexcount = 0;

	String myid;
    	ClientQueueElement element;
        String retlistid = null;
	String retnotlistid = null;
    //List<ResultItem> retList = null;
    //List<ResultItem> retNotList = null;
        String newsetid = null; 
//	MySet<String> newset = null; 
//	Map<String, HashSet<String>> dirset;
	String notfoundsetid;
	//boolean reindex = false;
	//boolean calculatenewmd5;
	boolean nomd5;

	   String[] dirlistnot;
	   SearchDisplay display;

    //Set<String> md5sdone = new HashSet<String>();
	
	public Traverse(String myid, ClientQueueElement element, String retlistid, String retnotlistid, String newsetid, String[] dirlistnot, String notfoundsetid, boolean nomd5) {

	    this.myid = myid;
	    this.element = element;
		this.retlistid = retlistid;
		this.retnotlistid = retnotlistid;
		this.newsetid = newsetid;
		this.notfoundsetid = notfoundsetid;
		//this.reindex = reindex;
		//this.calculatenewmd5 = newmd5;
		this.nomd5 = nomd5;
		
		this.dirlistnot = ControlService.dirlistnot;
		//UI ui = element.ui;
		//this.display = SearchService.getSearchDisplay(ui);
	}

    public static boolean isLocal(FileObject fo) {
        return FileSystemDao.exists(fo);
    }
	
}
