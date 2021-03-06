package com.dotcms.publisher.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.mapper.PublishAuditStatusMapper;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;


public class PublishAuditAPIImpl extends PublishAuditAPI {
	
	private static PublishAuditAPIImpl instance= null;
	private PublishAuditStatusMapper mapper = null;
	
	public static PublishAuditAPIImpl getInstance() {
		if(instance==null)
			instance = new PublishAuditAPIImpl();
		
		return instance;
	}
	
	protected PublishAuditAPIImpl(){
		// Exists only to defeat instantiation.
		mapper = new PublishAuditStatusMapper();
	}
	
	private final String MANDATORY_FIELDS= 
			"bundle_id, "+
			"status, "+ 
			"status_pojo, "+
			"status_updated, "+
			"create_date ";
	
	private final String MANDATORY_PLACE_HOLDER = "?,?,?,?,?" ;

	private final String INSERTSQL="insert into publishing_queue_audit("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	
	
	@Override
	public void insertPublishAuditStatus(PublishAuditStatus pa)
			throws DotPublisherException {
	    boolean localt=false;
		if(getPublishAuditStatus(pa.getBundleId()) == null) {
			try{
				localt=HibernateUtil.startLocalTransactionIfNeeded();
				DotConnect dc = new DotConnect();
				dc.setSQL(INSERTSQL);
				dc.addParam(pa.getBundleId());
				dc.addParam(pa.getStatus().getCode());
				
				dc.addParam(pa.getStatusPojo().getSerialized());
				dc.addParam(new Date());
				dc.addParam(new Date());
				
				dc.loadResult();
				
				if(localt) {
				    HibernateUtil.commitTransaction();
				}
			}catch(Exception e){
			    if(localt) {
    				try {
    					HibernateUtil.rollbackTransaction();
    				} catch (DotHibernateException e1) {
    					Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e1);
    				}			
			    }
				Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e);
				throw new DotPublisherException("Unable to add element to publish queue audit table:" + e.getMessage(), e);
			}
		}
	}
	
	private final String UPDATESQL="update publishing_queue_audit set status = ?, status_pojo = ?  where bundle_id = ? ";
	
	@Override
	public void updatePublishAuditStatus(String bundleId, Status newStatus, PublishAuditHistory history) throws DotPublisherException {
	    boolean local=false;
		try{
			local = HibernateUtil.startLocalTransactionIfNeeded();
			DotConnect dc = new DotConnect();
			dc.setSQL(UPDATESQL);
			dc.addParam(newStatus.getCode());
			
			if(history != null)
				dc.addParam(history.getSerialized());
			else
				dc.addParam("");
			
			dc.addParam(bundleId);
			
			
			dc.loadResult();
			
			if(local) {
			    HibernateUtil.commitTransaction();
			}
		}catch(Exception e){
		    if(local) {
    			try {
    				HibernateUtil.rollbackTransaction();
    			} catch (DotHibernateException e1) {
    				Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e1);
    			}
		    }
			Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException(
					"Unable to update element in publish queue audit table:" +
					"with the following bundle_id "+bundleId+" "+ e.getMessage(), e);
		}
	}
	
	private final String DELETESQL="delete from publishing_queue_audit where bundle_id = ? ";

	@Override
	public void deletePublishAuditStatus(String bundleId) throws DotPublisherException {
	    boolean local=false;
		try{
			local = HibernateUtil.startLocalTransactionIfNeeded();
			DotConnect dc = new DotConnect();
			dc.setSQL(DELETESQL);
			dc.addParam(bundleId);
			
			dc.loadResult();
			
			if(local) {
			    HibernateUtil.commitTransaction();
			}
		}catch(Exception e){
		    if(local) {
    			try {
    				HibernateUtil.rollbackTransaction();
    			} catch (DotHibernateException e1) {
    				Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e1);
    			}
		    }
			Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException(
					"Unable to remove element in publish queue audit table:" +
					"with the following bundle_id "+bundleId+" "+ e.getMessage(), e);
		}
	}
	
	private final String SELECTSQL=
			"SELECT * "+
			"FROM publishing_queue_audit a where a.bundle_id = ? ";
	
	@Override
	public PublishAuditStatus getPublishAuditStatus(String bundleId)
			throws DotPublisherException {
		
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECTSQL);
			
			dc.addParam(bundleId);
			
			List<Map<String, Object>> res = dc.loadObjectResults();
			if(res.size() > 1)
				throw new DotPublisherException("Found duplicate bundle status");
			else {
				if(!res.isEmpty())
					return mapper.mapObject(res.get(0));
				return null;
			}
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	private final String SELECTSQLALL=
			"SELECT * "+
			"FROM publishing_queue_audit order by status_updated desc";
	
	public List<PublishAuditStatus> getAllPublishAuditStatus() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECTSQLALL);
			
			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	public List<PublishAuditStatus> getAllPublishAuditStatus(Integer limit, Integer offset) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECTSQLALL);
			
			dc.setStartRow(offset);
			dc.setMaxRows(limit);
			
			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}
	
	
	private final String SELECTSQLMAXDATE=
			"select max(c.create_date) as max_date "+
			"from publishing_queue_audit c " +
			"where c.status != ? ";
	
	public Date getLastPublishAuditStatusDate() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECTSQLMAXDATE);
			
			dc.addParam(Status.BUNDLING.getCode());
			
			List<Map<String, Object>> res = dc.loadObjectResults();
			
			if(!res.isEmpty())
				return (Date) res.get(0).get("max_date");
			return null;
			
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	private final String SELECTSQLALLCOUNT=
			"SELECT count(*) as count "+
			"FROM publishing_queue_audit ";
	
	public Integer countAllPublishAuditStatus() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECTSQLALLCOUNT);
			return Integer.parseInt(dc.loadObjectResults().get(0).get("count").toString());
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}
	
	private final String SELECTSQLPENDING=
			"SELECT * "+
			"FROM publishing_queue_audit " +
			"WHERE status = ? or status = ? or status = ? or status = ? or status = ?";
	
	public List<PublishAuditStatus> getPendingPublishAuditStatus() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECTSQLPENDING);
			
			dc.addParam(PublishAuditStatus.Status.BUNDLE_SENT_SUCCESSFULLY.getCode());
			dc.addParam(PublishAuditStatus.Status.FAILED_TO_SEND_TO_SOME_GROUPS.getCode());
			dc.addParam(PublishAuditStatus.Status.FAILED_TO_SEND_TO_ALL_GROUPS.getCode());
			dc.addParam(PublishAuditStatus.Status.RECEIVED_BUNDLE.getCode());
			dc.addParam(PublishAuditStatus.Status.PUBLISHING_BUNDLE.getCode());
			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}
	
	public PublishAuditStatus updateAuditTable(String endpointId, String groupId, String bundleFolder)
			throws DotPublisherException {
		//Status
		PublishAuditStatus status =  new PublishAuditStatus(bundleFolder);
		//History
		PublishAuditHistory historyPojo = new PublishAuditHistory();
		EndpointDetail detail = new EndpointDetail();
		detail.setStatus(PublishAuditStatus.Status.RECEIVED_BUNDLE.getCode());
		detail.setInfo("Received bundle");
		
		historyPojo.addOrUpdateEndpoint(groupId, endpointId, detail);
		status.setStatus(PublishAuditStatus.Status.RECEIVED_BUNDLE);
		status.setStatusPojo(historyPojo);
		
		PublishAuditStatus existing=PublishAuditAPI.getInstance().getPublishAuditStatus(status.getBundleId());
		if(existing!=null) {
		    // update if there is an existing record.
		    PublishAuditAPI.getInstance().updatePublishAuditStatus(
		            status.getBundleId(), status.getStatus(), status.getStatusPojo());
		}
		else {
    		//Insert in Audit table
    		PublishAuditAPI.getInstance().insertPublishAuditStatus(status);
		}
		
		return status;
	}
}
