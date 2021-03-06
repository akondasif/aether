package roart.queue;

import java.io.InputStream;
import java.util.List;

import org.apache.tika.metadata.Metadata;

import roart.common.model.IndexFiles;
import roart.common.model.ResultItem;
import roart.common.model.SearchDisplay;

public class IndexQueueElement {
	public String type;
	public String md5;
    public String lang;
    // public String classification;
    public String content; // made from inputStream
	public InputStream inputStream;
	public volatile IndexFiles index;
    public String retlistid;
    public String retlistnotid;
	public int size;
	public String dbfilename;
    public Metadata metadata;
    public String convertsw;
	
    public IndexQueueElement(String type, String md5, InputStream inputStream, IndexFiles index, String retlistid, String retlistnotid, String dbfilename, Metadata metadata) {
	this.type = type;
	this.md5 = md5;
	this.inputStream = inputStream;
	this.index = index;
	this.retlistid = retlistid;
	this.retlistnotid = retlistnotid;
	this.dbfilename = dbfilename;
	this.metadata = metadata;
    }

}
