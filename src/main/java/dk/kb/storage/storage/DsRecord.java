package dk.kb.storage.storage;

public class DsRecord {

private String id;
private String base;
private boolean deleted;
private boolean indexable;
private String data;
private long cTime; //Internal value
private long mTime; //Internal value
private String parentId;


  public DsRecord(String id, String base, boolean indexable, String data, String parentId) {
		this.id=id;
		this.base=base;
		this.indexable=indexable;
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


	public boolean isIndexable() {
		return indexable;
	}


	public void setIndexable(boolean indexable) {
		this.indexable = indexable;
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


	
}
