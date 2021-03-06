package divconq.service.db;

import divconq.bus.IService;
import divconq.bus.Message;
import divconq.db.IDatabaseManager;
import divconq.db.ObjectFinalResult;
import divconq.db.ObjectResult;
import divconq.db.ReplicatedDataRequest;
import divconq.db.common.AddGroupRequest;
import divconq.db.common.AddUserRequest;
import divconq.db.common.RequestFactory;
import divconq.db.common.UpdateGroupRequest;
import divconq.db.common.UpdateUserRequest;
import divconq.db.common.UsernameLookupRequest;
import divconq.db.query.LoadRecordRequest;
import divconq.db.query.SelectDirectRequest;
import divconq.db.query.SelectFields;
import divconq.db.update.InsertRecordRequest;
import divconq.db.update.RetireRecordRequest;
import divconq.db.update.ReviveRecordRequest;
import divconq.db.update.UpdateRecordRequest;
import divconq.hub.Hub;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationContextBuilder;
import divconq.lang.op.UserContext;
import divconq.mod.ExtensionBase;
import divconq.struct.CompositeStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.work.TaskRun;

public class CoreDataServices extends ExtensionBase implements IService {
	@Override
	public void handle(TaskRun request) {
		Message msg = (Message) request.getTask().getParams();
		
		String feature = msg.getFieldAsString("Feature");
		String op = msg.getFieldAsString("Op");
		RecordStruct rec = msg.getFieldAsRecord("Body");
		
		OperationContext tc = OperationContext.get();
		UserContext uc = tc.getUserContext();
		
		IDatabaseManager db = Hub.instance.getDatabase();
		
		if (db == null) {
			request.errorTr(443);
			request.complete();
			return;
		}
		
		// =========================================================
		//  users
		// =========================================================
		
		if ("Users".equals(feature)) {
			if ("LoadSelf".equals(op) || "LoadUser".equals(op)) {
				LoadRecordRequest req = new LoadRecordRequest()
					.withTable("dcUser")
					.withId("LoadUser".equals(op) ? rec.getFieldAsString("Id") : uc.getUserId())
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcUsername", "Username")
						.withField("dcFirstName", "FirstName")
						.withField("dcLastName", "LastName")
						.withForeignField("dcGroup", "Groups", "dcName")
						.withField("dcEmail", "Email")
						.withField("dcBackupEmail", "BackupEmail")
						.withField("dcLocale", "Locale")
						.withField("dcChronology", "Chronology")
						.withField("dcDescription", "Description")
						.withField("dcConfirmed", "Confirmed")
						.withComposer("dcAuthorizationTags", "AuthorizationTags")
					);  
				
				
				db.submit(req, new ObjectFinalResult(request));
				
				return;
			}
						
			if ("UpdateSelf".equals(op) || "UpdateUser".equals(op)) {
				final UpdateUserRequest req = new UpdateUserRequest("UpdateUser".equals(op) ? rec.getFieldAsString("Id") : uc.getUserId());

				if (rec.hasField("Username"))
					req.setUsername(rec.getFieldAsString("Username"));

				if (rec.hasField("FirstName"))
					req.setFirstName(rec.getFieldAsString("FirstName"));

				if (rec.hasField("LastName"))
					req.setLastName(rec.getFieldAsString("LastName"));

				if (rec.hasField("Email"))
					req.setEmail(rec.getFieldAsString("Email"));

				if (rec.hasField("BackupEmail"))
					req.setBackupEmail(rec.getFieldAsString("BackupEmail"));

				if (rec.hasField("Locale"))
					req.setLocale(rec.getFieldAsString("Locale"));

				if (rec.hasField("Chronology"))
					req.setChronology(rec.getFieldAsString("Chronology"));
				
				if (rec.hasField("Password")) 
					req.setPassword(rec.getFieldAsString("Password")); 

				// not allowed for Self (see schema)
				if (rec.hasField("Confirmed")) 
					req.setConfirmed(rec.getFieldAsBoolean("Confirmed"));
				
				// not allowed for Self (see schema)
				if (rec.hasField("Description")) 
					req.setDescription(rec.getFieldAsString("Description"));
				
				// not allowed for Self (see schema)
				if (rec.hasField("AuthorizationTags"))
					req.setAuthorizationTags(rec.getFieldAsList("AuthorizationTags"));
				
				db.submit(req, new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("AddUser".equals(op)) {
				final AddUserRequest req = new AddUserRequest(rec.getFieldAsString("Username"));
			
				if (rec.hasField("FirstName"))
					req.setFirstName(rec.getFieldAsString("FirstName"));
			
				if (rec.hasField("LastName"))
					req.setLastName(rec.getFieldAsString("LastName"));
			
				if (rec.hasField("Email"))
					req.setEmail(rec.getFieldAsString("Email"));

				if (rec.hasField("BackupEmail"))
					req.setBackupEmail(rec.getFieldAsString("BackupEmail"));
			
				if (rec.hasField("Locale"))
					req.setLocale(rec.getFieldAsString("Locale"));
			
				if (rec.hasField("Chronology"))
					req.setChronology(rec.getFieldAsString("Chronology"));
				
				if (rec.hasField("Password")) 
					req.setPassword(rec.getFieldAsString("Password"));
				
				if (rec.hasField("Confirmed")) 
					req.setConfirmed(rec.getFieldAsBoolean("Confirmed"));
				else
					req.setConfirmed(true);
				
				if (rec.hasField("ConfirmCode")) 
					req.setConfirmCode(rec.getFieldAsString("ConfirmCode"));
				
				if (rec.hasField("Description")) 
					req.setDescription(rec.getFieldAsString("Description"));
				
				if (rec.hasField("AuthorizationTags"))
					req.setAuthorizationTags(rec.getFieldAsList("AuthorizationTags"));
				
				db.submit(req, new ObjectFinalResult(request));
				
				return;
			}
			
			if ("RetireSelf".equals(op) || "RetireUser".equals(op)) {
				db.submit(new RetireRecordRequest("dcUser", "RetireUser".equals(op) ? rec.getFieldAsString("Id") : uc.getUserId()),
						new ObjectResult() {
							@Override
							public void process(CompositeStruct result) {
								if ("RetireSelf".equals(op)) {
									// be sure we keep the domain id
									UserContext uc = request.getContext().getUserContext();
									
									OperationContext.switchUser(request.getContext(), new OperationContextBuilder()
										.withGuestUserTemplate()
										.withDomainId(uc.getDomainId())
										.toUserContext());
								}
								
								request.complete();
							}
						});
				
				return ;
			}
			
			if ("ReviveUser".equals(op)) {
				db.submit(new ReviveRecordRequest("dcUser", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("SetUserAuthTags".equals(op)) {
				ListStruct users = rec.getFieldAsList("Users");
				ListStruct tags = rec.getFieldAsList("AuthorizationTags");
				
				db.submit(RequestFactory.makeSet("dcUser", "dcAuthorizationTag", users, tags), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("AddUserAuthTags".equals(op)) {
				ListStruct users = rec.getFieldAsList("Users");
				ListStruct tags = rec.getFieldAsList("AuthorizationTags");
				
				db.submit(RequestFactory.addToSet("dcUser", "dcAuthorizationTag", users, tags), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("RemoveUserAuthTags".equals(op)) {
				ListStruct users = rec.getFieldAsList("Users");
				ListStruct tags = rec.getFieldAsList("AuthorizationTags");
				
				db.submit(RequestFactory.removeFromSet("dcUser", "dcAuthorizationTag", users, tags), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("UsernameLookup".equals(op)) {
				db.submit(new UsernameLookupRequest(rec.getFieldAsString("Username")), new ObjectFinalResult(request));
				
				return ;
			}

			// use with discretion
			if ("ListUsers".equals(op)) {
				db.submit(
					new SelectDirectRequest("dcUser", new SelectFields()
						.withField("Id")
						.withField("dcUsername", "Username")
						.withField("dcFirstName", "FirstName")
						.withField("dcLastName", "LastName")
						.withField("dcEmail", "Email")
					), 
					new ObjectFinalResult(request));
				
				return ;
			}
		}
		
		// =========================================================
		//  domains
		// =========================================================
		
		if ("Domains".equals(feature)) {
			if ("LoadDomain".equals(op) || "MyLoadDomain".equals(op)) {
				LoadRecordRequest req = new LoadRecordRequest()
					.withTable("dcDomain")
					.withId("MyLoadDomain".equals(op) ? uc.getDomainId() : rec.getFieldAsString("Id"))
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcTitle", "Title")
						.withField("dcDescription", "Description")
						.withField("dcName", "Names")
					);
				
				db.submit(req, new ObjectFinalResult(request));
				
				return;
			}
						
			if ("UpdateDomain".equals(op) || "MyUpdateDomain".equals(op)) {
				ReplicatedDataRequest req = new UpdateRecordRequest()
					.withTable("dcDomain")
					.withId("MyUpdateDomain".equals(op) ? uc.getDomainId() : rec.getFieldAsString("Id"))
					.withConditionallySetField(rec, "Title", "dcTitle")
					.withConditionallySetField(rec, "Description", "dcDescription")
					.withConditionallyReplaceList(rec, "Names", "dcName");
				
				db.submit(req, new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("AddDomain".equals(op)) {
				ReplicatedDataRequest req = new InsertRecordRequest()
					.withTable("dcDomain")
					.withConditionallySetField(rec, "Title", "dcTitle")
					.withConditionallySetField(rec, "Description", "dcDescription")
					.withConditionallySetField(rec, "ObscureClass", "dcObscureClass")				
					.withCopyList("dcName", true, rec.getFieldAsList("Names"));
				
				db.submit(req, new ObjectFinalResult(request));
				
				return;
			}
			
			if ("RetireDomain".equals(op)) {
				db.submit(new RetireRecordRequest("dcDomain", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("ReviveDomain".equals(op)) {
				db.submit(new ReviveRecordRequest("dcDomain", rec.getFieldAsString("Id")), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("SetDomainNames".equals(op) || "MySetDomainNames".equals(op)) {
				String did = "MySetDomainNames".equals(op) ? uc.getDomainId() : rec.getFieldAsString("Id");
				ListStruct names = rec.getFieldAsList("Names");
				
				db.submit(RequestFactory.makeSet("dcDomain", "dcName", did, names), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("AddDomainNames".equals(op) || "MyAddDomainNames".equals(op)) {
				String did = "MyAddDomainNames".equals(op) ? uc.getDomainId() : rec.getFieldAsString("Id");
				ListStruct names = rec.getFieldAsList("Names");
				
				db.submit(RequestFactory.addToSet("dcDomain", "dcName", did, names), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("RemoveDomainNames".equals(op) || "MyRemoveDomainNames".equals(op)) {
				String did = "MyRemoveDomainNames".equals(op) ? uc.getDomainId() : rec.getFieldAsString("Id");
				ListStruct names = rec.getFieldAsList("Names");
				
				db.submit(RequestFactory.removeFromSet("dcDomain", "dcName", did, names), new ObjectFinalResult(request));
				
				return ;
			}

			// use with discretion
			if ("ListDomains".equals(op)) {
				db.submit(
					new SelectDirectRequest("dcDomain", new SelectFields()
						.withField("Id")
						.withField("dcTitle", "Title")
						.withField("dcDescription", "Description")
						.withField("dcName", "Names")
					), 
					new ObjectFinalResult(request));
				
				return ;
			}
		}
		
		// =========================================================
		//  groups
		// =========================================================
		
		if ("Groups".equals(feature)) {
			if ("LoadGroup".equals(op)) {
				LoadRecordRequest req = new LoadRecordRequest()
					.withTable("dcGroup")
					.withId(rec.getFieldAsString("Id"))
					.withSelect(new SelectFields()
						.withField("Id")
						.withField("dcName", "Name")
						.withField("dcDescription", "Description")
						.withReverseForeignField("Users", "dcUser", "dcGroup", "dcUsername")
						.withField("dcAuthorizationTag", "AuthorizationTags")
					);
				
				db.submit(req, new ObjectFinalResult(request));
				
				return ;
			}
						
			if ("UpdateGroup".equals(op)) {
				final UpdateGroupRequest req = new UpdateGroupRequest(rec.getFieldAsString("Id"));

				if (rec.hasField("Name"))
					req.setName(rec.getFieldAsString("Name"));
				
				if (rec.hasField("Description")) 
					req.setDescription(rec.getFieldAsString("Description"));
				
				if (rec.hasField("AuthorizationTags"))
					req.setAuthorizationTags(rec.getFieldAsList("AuthorizationTags"));
				
				db.submit(req, new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("AddGroup".equals(op)) {
				final AddGroupRequest req = new AddGroupRequest(rec.getFieldAsString("Name"));
				
				if (rec.hasField("Description")) 
					req.setDescription(rec.getFieldAsString("Description"));
				
				if (rec.hasField("AuthorizationTags"))
					req.setAuthorizationTags(rec.getFieldAsList("AuthorizationTags"));
				
				db.submit(req, new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("RetireGroup".equals(op)) {
				db.submit(new RetireRecordRequest("dcGroup", rec.getFieldAsString("Id")), new ObjectFinalResult(request));				
				return ;
			}
			
			if ("ReviveGroup".equals(op)) {
				db.submit(new ReviveRecordRequest("dcGroup", rec.getFieldAsString("Id")), new ObjectFinalResult(request));				
				return ;
			}
			
			if ("SetGroupAuthTags".equals(op)) {
				final ListStruct groups = rec.getFieldAsList("Groups");
				final ListStruct tags = rec.getFieldAsList("AuthorizationTags");
				
				db.submit(RequestFactory.makeSet("dcGroup", "dcAuthorizationTag", groups, tags), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("AddGroupAuthTags".equals(op)) {
				final ListStruct groups = rec.getFieldAsList("Groups");
				final ListStruct tags = rec.getFieldAsList("AuthorizationTags");
				
				db.submit(RequestFactory.addToSet("dcGroup", "dcAuthorizationTag", groups, tags), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("RemoveGroupAuthTags".equals(op)) {
				final ListStruct groups = rec.getFieldAsList("Groups");
				final ListStruct tags = rec.getFieldAsList("AuthorizationTags");
				
				db.submit(RequestFactory.removeFromSet("dcGroup", "dcAuthorizationTag", groups, tags), new ObjectFinalResult(request));
				
				return ;
			}

			// use with discretion
			if ("ListGroups".equals(op)) {
				db.submit(
					new SelectDirectRequest("dcGroup", new SelectFields()
						.withField("Id")
						.withField("dcName", "Name")
					), 
					new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("SetUsersToGroups".equals(op)) {
				final ListStruct groups = rec.getFieldAsList("Groups");
				final ListStruct users = rec.getFieldAsList("Users");
				
				db.submit(RequestFactory.makeSet("dcUser", "dcGroup", users, groups), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("AddUsersToGroups".equals(op)) {
				final ListStruct groups = rec.getFieldAsList("Groups");
				final ListStruct users = rec.getFieldAsList("Users");
				
				db.submit(RequestFactory.addToSet("dcUser", "dcGroup", users, groups), new ObjectFinalResult(request));
				
				return ;
			}
			
			if ("RemoveUsersFromGroups".equals(op)) {
				final ListStruct groups = rec.getFieldAsList("Groups");
				final ListStruct users = rec.getFieldAsList("Users");
				
				db.submit(RequestFactory.removeFromSet("dcUser", "dcGroup", users, groups), new ObjectFinalResult(request));
				
				return ;
			}
		}
		
		request.errorTr(441, this.serviceName(), feature, op);
		request.complete();
	}
}
