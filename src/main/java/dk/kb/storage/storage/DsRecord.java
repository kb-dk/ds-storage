package dk.kb.storage.storage;

import java.util.ArrayList;

public class DsRecord {

private String id;
private String base;
private boolean deleted;
private String data;
private long cTime; //Internal value
private long mTime; //Internal value
private ArrayList<String> children = new  ArrayList<String>();  
private String parentId;


  public DsRecord(String id, String base,  String data, String parentId) {
		this.id=id;
		this.base=base;		
		this.data=data;
		this.parentId=parentId;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public String getBase() {
		return base;
	}


	public void setBase(String base) {
		this.base = base;
	}


	public boolean isDeleted() {
		return deleted;
	}


	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	
	public String getData() {
		return data;
	}


	public void setData(String data) {
		this.data = data;
	}


	public long getcTime() {
		return cTime;
	}


	public void setcTime(long cTime) {
		this.cTime = cTime;
	}


	public long getmTime() {
		return mTime;
	}


	public void setmTime(long mTime) {
		this.mTime = mTime;
	}


	public String getParentId() {
		return parentId;
	}


	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public ArrayList<String> getChildren() {
		return children;
	}


	public void setChildren(ArrayList<String> children) {
		this.children = children;
	}


	@Override
	public String toString() {
		return "DsRecord [id=" + id + ", base=" + base + ", deleted=" + deleted + ", data=" + data + ", cTime=" + cTime
				+ ", mTime=" + mTime + ", #children=" + children.size()+ ", parentId=" + parentId + "]";
	}



	
}
