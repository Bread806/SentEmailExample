package com.efgp.sb;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;


import com.dsc.nana.domain.SystemConfig;
import com.dsc.nana.domain.form.FormInstance;
import com.dsc.nana.services.SystemConfigManager;
import com.dsc.nana.services.exception.NotFoundException;
import com.dsc.nana.services.exception.ServiceException;
import com.dsc.nana.services.util.SessionBeanHelper;
import com.dsc.nana.util.IMailUtil;
import com.dsc.nana.util.MailUtil;
import com.dsc.nana.util.SystemException;
import com.dsc.nana.util.logging.NaNaLog;
import com.dsc.nana.util.logging.NaNaLogFactory;


/**
 * Session Bean implementation class SendMailBean
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class SendMailBean implements SendMail {
	
	/** 寫 Log 的工具 */
	private NaNaLog log;

	/**
	 * 屬性log的 getter
	 * 
	 * @author Hsieh
	 * @since NaNa 0.7.0
	 */
	private NaNaLog getLog() {
		if (this.log == null) {
			this.log = NaNaLogFactory.getLog(SendMailBean.class);
		}
		return this.log;
	}
	
	private Context containerContext;
	
    /**
     * Default constructor. 
     */
    public SendMailBean() {
        // TODO Auto-generated constructor stub
    }
    
    public void sendMail() {
		DataSource tDataSource = null;
		tDataSource = getJndiDataSource("NaNaCustDS");//取得 DS
		Connection tConnection = null;
		PreparedStatement tPreparedStatement = null;
		ResultSet tResultSetYesterday = null;
		ResultSet tResultSetToday = null;
		int tResultYesterdayLength = 0;
		int tResultTodayLength  = 0;
		ArrayList<QueryData> YesterdayQueryDataList =new ArrayList<>();
		ArrayList<QueryData> TodayQueryDataList =new ArrayList<>();
		
		SystemConfigManager tParameter=null;
		try {
			//57 取得Mail server 資訊
			tParameter = SessionBeanHelper.getInstance().lookup(SystemConfigManager.class);
		} catch (EJBException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
        SystemConfig tParameterConfig=null;
		try {
			tParameterConfig = tParameter.getSystemConfigDTO();
		} catch (ServiceException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String MailServerAccount = tParameterConfig.getMailServerAccount();
		//System.out.println("MailServerAccount:"+MailServerAccount);
        String MailServerAddress = tParameterConfig.getMailServerAddress();
        //System.out.println("MailServerAddress:"+MailServerAddress);
        String MailServerPwd = tParameterConfig.getMailServerPwd();
        //System.out.println("MailServerPwd:"+MailServerPwd);
        String DefaultSender = tParameterConfig.getDefaultSender();
       // System.out.println("DefaultSender:"+DefaultSender);
        //System.out.println("MailServerAccount:"+MailServerAccount+"\nMailServerAddress:"+MailServerAddress+"\nMailServerPwd:"+MailServerPwd+"\nDefaultSender:"+DefaultSender);
		
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd");
		Date currentDate = new Date();
		String strTodayDate = sdFormat.format(currentDate);
		Long yesterdayTime = currentDate.getTime() - (24 * 60 * 60 * 1000);
		Date yesterdayDate = new Date(yesterdayTime);
		String strYesterdayDate = sdFormat.format(yesterdayDate);
		
		try {
			tConnection = tDataSource.getConnection();
			StringBuilder tSQLBuilder = new StringBuilder();
		
			// build yesterday SQL
			tSQLBuilder.append("select APPLYDATE AS 申請日期, ");
			tSQLBuilder.append("APPLYEMPCD AS 申請人員編, ");
			tSQLBuilder.append("APPLYNM AS 申請人姓名, ");
			tSQLBuilder.append("SerialNumber as 單號, ");
			tSQLBuilder.append("reason as 拆閱原因  ");
			tSQLBuilder.append("from OBANK_IT_D11 ");
			tSQLBuilder.append("where APPLYDATE = CONVERT(VARCHAR, DATEADD(DD, -1, GETDATE()), 111)");
			System.out.println("tSQL : "+tSQLBuilder.toString());
			
			// build sql command done. Try to execite query command.
			tPreparedStatement = tConnection.prepareStatement(tSQLBuilder.toString());
			tResultSetYesterday = tPreparedStatement.executeQuery();
			
			// get result length
			while(tResultSetYesterday.next()) {
				tResultYesterdayLength++;
				QueryData queryData = new QueryData();
				queryData.set_tApplyDate(tResultSetYesterday.getString("申請日期"));
				queryData.set_tApplyUserId(tResultSetYesterday.getString("申請人員編"));
				queryData.set_tApplyUserName(tResultSetYesterday.getString("申請人姓名"));
				queryData.set_tApplySerialNumber(tResultSetYesterday.getString("單號"));
				queryData.set_tApplyReason(tResultSetYesterday.getString("拆閱原因"));
				YesterdayQueryDataList.add(queryData);
			}
			
			// clear StringBuilder
			tSQLBuilder.setLength(0);
			
			// build today SQL
			tSQLBuilder.append("select APPLYDATE AS 申請日期, ");
			tSQLBuilder.append("APPLYEMPCD AS 申請人員編, ");
			tSQLBuilder.append("APPLYNM AS 申請人姓名, ");
			tSQLBuilder.append("SerialNumber as 單號, ");
			tSQLBuilder.append("reason as 拆閱原因  ");
			tSQLBuilder.append("from OBANK_IT_D11 ");
			tSQLBuilder.append("where APPLYDATE = CONVERT(VARCHAR, GETDATE(), 111)");
			System.out.println("tSQL : "+tSQLBuilder.toString());
			
			// build sql command done. Try to execite query command.
			tPreparedStatement = tConnection.prepareStatement(tSQLBuilder.toString());
			System.out.println("A");
			tResultSetToday = tPreparedStatement.executeQuery();
			System.out.println("B");
			
			// get result length
			while(tResultSetToday.next()) {
				tResultTodayLength++;
				QueryData queryData = new QueryData();
				queryData.set_tApplyDate(tResultSetToday.getString("申請日期"));
				queryData.set_tApplyUserId(tResultSetToday.getString("申請人員編"));
				queryData.set_tApplyUserName(tResultSetToday.getString("申請人姓名"));
				queryData.set_tApplySerialNumber(tResultSetToday.getString("單號"));
				queryData.set_tApplyReason(tResultSetToday.getString("拆閱原因"));
				TodayQueryDataList.add(queryData);	
			}
			
			
			// prepare mail necessary item
			String tSubject = (	strYesterdayDate + " 機房密碼函拆閱備查登記表"); // 主旨
			String tAssigners = "breadwu@digiwin.com";  				// 測試信箱
			MailUtil tMailUtil = new MailUtil();
			StringBuilder content = new StringBuilder();
			
			
			
			// [SQL1] > 0
			if (tResultYesterdayLength > 0) {
				content.append("<div><p style=\"display: flex;justify-content: center;align-items: center;\">" + "今日機房有拆閱碼單，請值班安管發出調查信件追蹤並登記於【CA PIM】每日特權帳號使用調查表。" + "</p></div>");
				content.append("<div><p style=\"display: flex;justify-content: center;align-items: center;\">" + strYesterdayDate + "</p></div>");
				content.append(wrap_mail_content(YesterdayQueryDataList));
				content.append("<br>");
				content.append("<div><p style=\"display: flex;justify-content: center;align-items: center;\">" + strTodayDate + "</p></div>");
				
				/*昨日與今日都有資料*/
				if (tResultTodayLength > 0) {										
					content.append(wrap_mail_content(TodayQueryDataList));						
				}
				/*昨日有資料，今日沒有資料*/
				else {
					content.append("<p style=\"display: flex;justify-content: center;align-items: center;\">" + "無資料" + "</p>");

				}
				
			}	
				
			else {
				content.append("<p style=\"display: flex;justify-content: center;align-items: center;\">" + "今日無機房拆閱碼單紀錄。" + "</p>");
				content.append("<p style=\"display: flex;justify-content: center;align-items: center;\">" + strYesterdayDate + "</p>");
				content.append("<p style=\"display: flex;justify-content: center;align-items: center;\">" + "無資料" + "</p>");
				content.append("<br>");
				content.append("<div><p style=\"display: flex;justify-content: center;align-items: center;\">" + strTodayDate + "</p></div>");
				
				/*昨日沒有資料，今日有資料*/
				if (tResultTodayLength > 0) {
					content.append(wrap_mail_content(TodayQueryDataList));
				}
				/*昨日與今日都沒有資料*/
				else {
					content.append("<p style=\"display: flex;justify-content: center;align-items: center;\">" + "無資料" + "</p>");
				}
			}
			try {
				tMailUtil.sendMail(MailServerAddress, 	//伺服器IP
				                  MailServerAccount,	//系統連線帳號
				                  MailServerPwd,		//系統連線密碼
				                  tSubject,				//主旨
				                  content.toString(),	//內文
				                  tAssigners,			//收件者
				                  DefaultSender);		//寄件人
				
				if(this.getLog().isDebugEnabled()){
					this.getLog().debug("主旨 : " + tSubject + "\r\n內文  : " + content);
				}
				System.out.println("主旨 : " + tSubject + "\r\n內文  : " + content);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				if(this.getLog().isErrorEnabled()){
	 				this.getLog().error("(" + this.hashCode() + ")Fail!!ErrMsg:" + e );
	 			}
			}
				
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			if(this.getLog().isErrorEnabled()){
				this.getLog().error("(" + this.hashCode() + ")Fail!!ErrMsg:" + e );
			}
			//throw new ServiceException(e);
		}finally{
			closeResultSet(tResultSetYesterday);
			closeResultSet(tResultSetToday);
			closePreparedStmt(tPreparedStatement);
			closeConn(tConnection);
		}
		
	}
	
    
	private StringBuilder wrap_mail_content(ArrayList<QueryData> tResultSet) {
		StringBuilder content = new StringBuilder();
		
		// building content
		content.append(
				"<!DOCTYPE html>\r\n" + 
				"<html>\r\n" + 
				"<head>\r\n" + 
				"    <title>Table Example</title>\r\n" + 
				"		<style>\r\n" + 
				"			table{\r\n" + 
				"				border-collapse: collapse;\r\n" + 
				"				text-align: left;\r\n" + 
				"			}\r\n" + 
				"			th, td {\r\n" + 
				"            border: 1px solid black;\r\n" + 
				"            padding: 8px;\r\n" + 
				"			text-align: left;\r\n" +
				"			width: 125px;\r\n" + 
				"			height: 25px;"+
				"			}\r\n" + 
				"			.container {\r\n" + 
				"				display: flex;\r\n" + 
				"				justify-content: center;\r\n" + 
				"				align-items: center;\r\n" + 
				"			}\r\n" + 
				"		</style>\r\n" + 
				"</head>\r\n" + 
				"<body>\r\n" + 
				"		<div class = \"container\">\r\n" + 
				"			<table>\r\n" + 
				"					<tr>\r\n" + 
				"							<th>申請日期</th>\r\n" + 
				"							<th>申請人(員編)</th>\r\n" + 
				"							<th>申請人(姓名)</th>\r\n" + 
				"							<th>單號</th>\r\n" + 
				"							<th>拆閱原因</th>\r\n" + 
				"					</tr>");
		
		// append content
		try {
			for (int i=0 ; i<tResultSet.size() ; i++) {
				content.append("<tr>\r\n" + 
						"            <td>" + tResultSet.get(i).get_tApplyDate() + "</td>\r\n" + 
						"            <td>" + tResultSet.get(i).get_tApplyUserId() + "</td>\r\n" + 
						"            <td>" + tResultSet.get(i).get_tApplyUserName() + "</td>\r\n" + 
						"            <td>" + tResultSet.get(i).get_tApplySerialNumber() + "</td>\r\n" + 
						"            <td>" + tResultSet.get(i).get_tApplyReason() + "</td>\r\n" + 
						"        </tr>");
			}	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // append content done
		finally {
			content.append(
					"    </table>\r\n" + 
					"</div>\r\n" + 
					"</body>\r\n" + 
					"</html>");	
		}
		
		return content ;
	}
	
	public class QueryData{
		
		private String tApplyDate;
		private String tApplyUserId;
		private String tApplyUserName;
		private String tApplySerialNumber;
		private String tApplyReason;
		
		public QueryData() {
			tApplyDate = null;
			tApplyUserId = null;
			tApplyUserName = null;;
			tApplySerialNumber = null;;
			tApplyReason = null;;
		}
		
		public void set_tApplyDate(String tApplyDate) {
			this.tApplyDate = tApplyDate;
		}
		public void set_tApplyUserId(String tApplyUserId) {
			this.tApplyUserId = tApplyUserId;
		}
		public void set_tApplyUserName(String tApplyUserName) {
			this.tApplyUserName = tApplyUserName;
		}
		public void set_tApplySerialNumber(String tApplySerialNumber) {
			this.tApplySerialNumber = tApplySerialNumber;
		}
		public void set_tApplyReason(String tApplyReason) {
			this.tApplyReason = tApplyReason;
		}
		public String get_tApplyDate() {
			return this.tApplyDate;
		}
		public String get_tApplyUserId() {
			return this.tApplyUserId;
		}
		public String get_tApplyUserName() {
			return this.tApplyUserName;
		}
		public String get_tApplySerialNumber() {
			return this.tApplySerialNumber;
		}
		public String get_tApplyReason() {
			return this.tApplyReason;
		}
		
		
	}
    
 // 取得 DataSource
 	private DataSource getJndiDataSource(String pDataSource)throws ServiceException {
 		
 		String tFormalDS = "java:/" + pDataSource;
 		DataSource tDs = null;
 		Context tCtx = null;
 		try {
 			tCtx = new InitialContext();
 			tDs = (DataSource) tCtx.lookup(tFormalDS);
 		} catch (NamingException e) {
 			if (this.getLog().isErrorEnabled()) {
 				this.getLog().error("Get data source  " + pDataSource + " error.ErrMsg:"
 						+ e.getMessage());
 			}
 			throw new ServiceException(e.getMessage());
 		} finally {
 			if (tCtx != null) {
 				try {
 					tCtx.close();
 				} catch (Exception ex) {
 					ex.printStackTrace();
 				}
 				tCtx = null;
 			}
 		}
 		if (tDs == null) {
 			throw new ServiceException("Can not get DataSource from jndi name "
 					+ pDataSource);
 		}
 		return tDs;
 	}
 	
 	private void closeConn(Connection pConn) {
 		if (pConn != null) {
 			try {
 				pConn.close();
 			}
 			catch (SQLException e) {
 				if(this.getLog().isErrorEnabled()){
 					this.getLog().error("(" + this.hashCode() + ")Close PreparedStatement Fail!!ErrMsg:" + e.getMessage());
 				}
 			}
 		}
 	}


 	private void closePreparedStmt(Statement pPreparedStmt) {
 		if (pPreparedStmt != null) {
 			try {
 				pPreparedStmt.close();
 			}
 			catch (SQLException e) {
 				if(this.getLog().isErrorEnabled()){
 					this.getLog().error("(" + this.hashCode() + ")Close PreparedStatement Fail!!ErrMsg:" + e.getMessage());
 				}
 			}
 		}
 	}
 	
 	private void closeResultSet(ResultSet pRs) {
 		if (pRs != null) {
 			try {
 				pRs.close();
 			}
 			catch (SQLException e) {
 				if (this.getLog().isErrorEnabled()){ 
 					this.getLog().error("(" + this.hashCode() + ")Close PreparedStatement Fail!!ErrMsg:" + e.getMessage());
 				}
 			}
 		}
 	}
 	
 	 private Context getContainerContext() throws NamingException {
          if (containerContext == null) {
                  Context tContext = new InitialContext();
                  containerContext = tContext;
          }
          return containerContext;
 	 }
    
}
