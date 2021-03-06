package com.funambol.LDAP.manager;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.funambol.LDAP.BaseTestCase;
import com.funambol.LDAP.dao.ContactDAOInterface;
import com.funambol.LDAP.dao.impl.ContactDAO;
import com.funambol.LDAP.dao.impl.PersonContactDAO;
import com.funambol.LDAP.dao.impl.PiTypeContactDAO;
import com.funambol.LDAP.dao.impl.PiTypeContactDAOTest;
import com.funambol.LDAP.exception.LDAPAccessException;
import com.funambol.LDAP.manager.impl.OpenLdapInterface;
import com.funambol.LDAP.utils.Constants;
import com.funambol.framework.engine.SyncItem;
import com.funambol.framework.engine.SyncItemImpl;
import com.funambol.framework.engine.source.SyncSourceException;

public class OpenLdapInterfaceTest extends BaseTestCase  {


	public OpenLdapInterface ldapInterface;
	private String ENTRY_UID = "123-123-123-123";

	String LDAP_URI = "ldap://openldap.babel.it/";
	String ROOT_DN = "uid=rpolli,ou=people,o=babel.it";

	protected String USER_BASEDN = "uid=rpolli,ou=people,o=babel.it"; // "ou=people, dc=bigdomain.net,o=bigcompany," + ROOT_DN;
	private static String USER_DN = "uid=rpolli,ou=people,o=babel.it" ; // "uid=Aaccf.Amar,ou=People,dc=bigdomain.net,o=bigcompany,dc=babel,dc=it";
	private static String USER_PASS = "password";
	private static String USER_MAIL = "a@a.it"; // "aaccf.amar@bigdomain.net";
	private   String PSROOT = ROOT_DN; // "ou=Aaccf.Amar@bigdomain.net, dc=bigdomain.net, dc=PAB";

	private  ContactDAOInterface piTypeCdao = new PiTypeContactDAO();
	private  ContactDAOInterface personCdao = new PersonContactDAO();
	private  ContactDAOInterface standardCdao = new ContactDAO();


	private List<String> addedEntries;

	@Override
	@Before
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
		addedEntries = new ArrayList<String>();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		addedEntries.add("(piEntryId=-2)");
		addedEntries.add("(cn=Roberto Polli)");
		for (String filter : addedEntries) {
			try {
				for (String dn : ldapInterface.searchDn(filter, SearchControls.SUBTREE_SCOPE) ) {
					logger.info("deleting entry: "+ dn);
					ldapInterface.delete(dn, false);	
					//addedEntries.remove(filter);					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * test various ways of constructing LdapManager
	 */

	@Test
	public void testConstructor() {
		// test variuos type of manager constructor

		// direct constructor without dao
		try {
			ldapInterface = new OpenLdapInterface(LDAP_URI, ROOT_DN, null, null, false, false);
			String ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);
			assertNotNull(ldapInterface.getContext());			
			assertNotNull(ldapId);
			ldapInterface.close();


			// ... with dao without key
			ldapInterface = new OpenLdapInterface(LDAP_URI, ROOT_DN, null, null, false, false, this.standardCdao);
			ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);
			assertNotNull(ldapInterface.getContext());	
			assertNotNull(ldapId);
			ldapInterface.close();
			// .. with complex dao
			ldapInterface = new OpenLdapInterface(LDAP_URI, ROOT_DN, null, null, false, false, this.piTypeCdao);
			ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);
			assertNotNull(ldapInterface.getContext());	
			assertNotNull(ldapId);
			ldapInterface.close();

			// bean constructor without init
			ldapInterface = new OpenLdapInterface();
			// we shouldn't be able to set context
			try {
				assertNull(ldapInterface.getContext());
				fail("With empty constructor we should not be able to raise context");
			} catch (LDAPAccessException e) {
				// noop
			} finally {
				ldapInterface.close();
			}
			assertNotNull(ldapInterface);
			ldapInterface.close();

			// then with init
			ldapInterface.init(LDAP_URI, ROOT_DN, null, null, false, false, null);
			ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);

			assertNotNull(ldapInterface.getContext());	
			assertNotNull(ldapId);
			ldapInterface.close();

			// then re-initializing
			ldapInterface.init(LDAP_URI, ROOT_DN, null, null, false, false,piTypeCdao);
			ldapId = ldapInterface.getLdapId(); 
			logger.info("Ldapid is: " + ldapId);
			assertNotNull(ldapInterface.getContext());	
			assertNotNull(ldapId);

			// and again
			ldapInterface.init(LDAP_URI, ROOT_DN, null, null, false, false,piTypeCdao);
			ldapId = ldapInterface.getLdapId(); 
			ldapInterface.setLdapId(ldapInterface.getCdao().getRdnAttribute());
			logger.info("Ldapid is: " + ldapInterface.getLdapId());
			assertNotNull(ldapInterface.getContext());	
			assertNotSame(ldapId, ldapInterface.getLdapId());

		} catch (LDAPAccessException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * test with auth binding
	 * @throws SyncSourceException 
	 */
	@Test
	public void testAnonSearchDn() throws LDAPAccessException {
		// create anonymous ldapinterface context
		ldapInterface = new OpenLdapInterface(LDAP_URI, ROOT_DN, null, null, false, false);

		// search dn by filter
		String filter = String.format(AbstractLDAPManager.BASIC_FILTER, "mail", USER_MAIL);
		List<String> dn = ldapInterface.searchDn(filter, SearchControls.SUBTREE_SCOPE);

		// check
		assertNotNull(dn); 
		assertEquals(1, dn.size());
	}
	@Test
	public void testAuthSearchDn() throws LDAPAccessException {
		// create anonymous ldapinterface context
		ldapInterface = new OpenLdapInterface(LDAP_URI, ROOT_DN, USER_DN, USER_PASS, false, false);

		// search dn by filter
		String filter = String.format(AbstractLDAPManager.BASIC_FILTER, "mail", USER_MAIL);
		List<String> dn = ldapInterface.searchDn(filter, SearchControls.SUBTREE_SCOPE);

		// check
		assertNotNull(dn); 
		assertEquals(1, dn.size());
	}
	/**
	 * Retrieve one entry from user's psRoot
	 * @throws SyncSourceException 
	 * @throws NamingException 
	 */
	public void _testSearchOneEntry() throws Exception {
		logger.info("testSearchOneEntry");

		// retrieve attrs from DN
		ldapInterface = new OpenLdapInterface(LDAP_URI, USER_BASEDN, USER_DN, USER_PASS, false, false);

		Attributes attrs =  (ldapInterface.searchOneEntry("(objectclass=*)", 
				new String[] { "dn", "psRoot"} , 
				SearchControls.ONELEVEL_SCOPE).getAttributes());
		logger.trace(attrs);
		assertNotNull(attrs.get("psRoot"));
		String psroot = (String) attrs.get("psRoot").get();


		ldapInterface = null;
		ldapInterface = new OpenLdapInterface(psroot,"", null,null, false, false);

		List<String> entries = ldapInterface.getAllUids();
		logger.info("found #entries: " + entries.size());
		assertNotSame(0, entries.size());

	}
	/** 
	 * retrieve all entries
	 */
	@Test
	public void testAuthGetAllUids() {
		try {
			logger.info("testGetAllEntries");
			ldapInterface = (OpenLdapInterface) LDAPManagerFactory.createLdapInterface(SERVER_OPENLDAP);
			ldapInterface.init(LDAP_URI, USER_BASEDN, USER_BASEDN, USER_PASS, false,false, standardCdao);

			List<String> allUids = ldapInterface.getAllUids();
			logger.info("found #entries:" + allUids.size());
			assertNotSame(0, allUids.size());

		} catch (LDAPAccessException e) {
			fail(e.getMessage());
		} catch (SyncSourceException e) {
			e.printStackTrace();
		}
	}
	@Test
	public void testAuthCacheAllEntries() {
		logger.info("testGetAllEntries");
		try {
			ldapInterface = (OpenLdapInterface) LDAPManagerFactory.createLdapInterface(Constants.SERVER_OPENLDAP);
			ldapInterface.init(LDAP_URI, USER_BASEDN, USER_BASEDN, USER_PASS, false, false, standardCdao);
			HashMap<String, Attributes> allEntries = ldapInterface.getAllEntries("objectclass=person");

			// logger.info(allEntries);
		} catch (LDAPAccessException e) {
			fail(e.getMessage());
		} catch (SyncSourceException e) {
			fail(e.getMessage());
		} 					
	}

	//	/**
	//	 * are there performance issues between list and hashmap?
	//	 */
	//	
	@Test
	@Ignore
	public void testCacheAllEntriesAsList() {
		//		logger.info("testGetAllEntries");
		//		try {
		//			ldapInterface = (OpenLdapInterface) LDAPManagerFactory.getLdapInterface(SERVER_OPENLDAP);
		//			ldapInterface.init(LDAP_URI, USER_BASEDN, null, null, false, piTypeCdao);
		//			List<Attributes> allEntries = ldapInterface.getAllEntriesAsList("objectclass=person");
		//
		//			// logger.info(allEntries);
		//		} catch (SyncSourceException e) {
		//			fail(e.getMessage());
		//		} 					
		//	
		}

		/**
		 * add, update, remove an entry, needs DAO
		 * @throws NamingException 
		 * @throws SyncSourceException 
		 */
		@Test
		public void testAddUpdateRemove() throws Exception {
			try {
				logger.info("testAddUpdateRemove");

				for (ContactDAOInterface dao : new ContactDAOInterface[] {standardCdao
						// , personCdao
						//, piTypeCdao
				} ) {
					ldapInterface = new OpenLdapInterface(this.LDAP_URI, this.USER_BASEDN, DM_USER, DM_PASS, false, false,dao);
					Attributes entryAttributes;

					String mailAttribute;
					if (dao instanceof com.funambol.LDAP.dao.impl.PiTypeContactDAO ) {
						// if DAO provides a custom attribute for storing UID, retrieve item from psRoot and use it as a key

						Attributes attrs =  (ldapInterface.searchOneEntry("(objectclass=*)", 
								new String[] { "dn", "psRoot"} , 
								SearchControls.ONELEVEL_SCOPE).getAttributes());
						logger.trace(attrs);
						assertNotNull(attrs.get("psRoot"));
						String psRoot = (String) attrs.get("psRoot").get();
						ldapInterface.close();
						ldapInterface.init(psRoot, "", DM_USER, DM_PASS, false,false);

						ldapInterface.setLdapId(dao.getRdnAttribute());

						mailAttribute = "piEmail1";
						entryAttributes = PiTypeContactDAOTest.getMockEntry();

					} else {
						entryAttributes = PiTypeContactDAOTest.getMockSimpleEntry();
						mailAttribute = "mail";
					}


					String myKey = ldapInterface.addNewEntry(entryAttributes);
					addedEntries.add(String.format("(%s=%s)", ldapInterface.getLdapId(), myKey));
					assertNotNull(myKey);

					entryAttributes.remove(mailAttribute);
					entryAttributes.put(mailAttribute, "newmail@babel.it");

					ldapInterface.updateEntry(myKey, entryAttributes);

					assertEquals("newmail@babel.it", (String) ldapInterface.searchLDAPEntryById(myKey).getAttributes().get(mailAttribute).get() );
					SyncItem si = new SyncItemImpl(null, myKey);			
					ldapInterface.deleteEntry(si, false);
				}
			} catch (SyncSourceException e) {
				fail("Test Error: "+ e.getMessage());
			} catch (NamingException e) {
				fail("LDAP Error: "+ e.getMessage());
			} 

		}


		public void _testAddAndSoftDelete() {
			String idfilter = "";
			try {
				// work only with PiTypeContactDAO
				PiTypeContactDAO dao =  (PiTypeContactDAO) piTypeCdao;

				ldapInterface = new OpenLdapInterface("ldap://be-mmt.babel.it/", PSROOT, DM_USER, DM_PASS, false,false, dao);
				ldapInterface.setLdapId(dao.getRdnAttribute());

				Attributes entryAttributes = PiTypeContactDAOTest.getMockEntry();

				String myKey = ldapInterface.addNewEntry(entryAttributes);
				assertNotNull(myKey);
				idfilter= "("+ldapInterface.getLdapId()+"="+dao.getRdnValue(entryAttributes)+")";
				String dn = ldapInterface.searchDn(idfilter, SearchControls.SUBTREE_SCOPE).get(0);

				ldapInterface.delete(dn, true);

				assertEquals(0,ldapInterface.searchDn("(& (!("+ dao.getSoftDeleteAttribute()+"=1)) (objectclass=person)"+idfilter+")", SearchControls.SUBTREE_SCOPE).size());
				assertNotNull(ldapInterface.searchDn(idfilter, SearchControls.SUBTREE_SCOPE).get(0));
			} catch (LDAPAccessException e) {
				fail(e.getMessage());
			} catch (NameAlreadyBoundException e) {
				fail(e.getMessage());
			} finally {
				// delete entry
				try {
					String dn = ldapInterface.searchDn(idfilter, SearchControls.SUBTREE_SCOPE).get(0);
					logger.info("deleting entry: "+ dn);
					ldapInterface.delete(dn, false);
				} catch (Exception e) {
					// spada
				}
			}
		}


		@Test
		public void testSearchSoftDelete() {

		}
		/**
		 * add 2 entries
		 * update 1 entry
		 * retrieve new and updated
		 */
		@Test
		public void testAddUpdateAndGetNewUpdated() {

		}


		/**
		 * this should create a new syncItemKey to be returned
		 * @param si
		 * @return
		 */
		@Test
		public void testAddUpdateRemoveSyncItem() {
			try {
				ldapInterface = new OpenLdapInterface("ldap://be-mmt.babel.it/"+PSROOT, "", DM_USER, DM_PASS, false,false, piTypeCdao);			
				ldapInterface.setLdapId(ldapInterface.getCdao().getRdnAttribute());
				String vcards[] = { 
						"vcard-1.vcf"
						, "vcard-2.vcf","vcard-3.vcf", "vcard-4.vcf", "vcard-5.vcf"
				};

				Timestamp t0    = new Timestamp(System.currentTimeMillis());
				for (String vcf : vcards) {
					try {
						SyncItem item = getResourceAsSyncItem(FCTF_BASIC +  vcf, TYPE_VCF2);
						String key = ldapInterface.addNewEntry(item);
						addedEntries.add(String.format("(%s=%s)", ldapInterface.getLdapId(), key));
						assertNotNull(key);

						item.setTimestamp(t0);
						item.getKey().setKeyValue(key);
						ldapInterface.updateEntry(item);



					} catch (Exception e) {
						logger.error("missing file: "+ e.getMessage());
					}
				}

			} catch (LDAPAccessException e) {
				fail(e.getMessage());
			}

		}


		@Test
		public void testOpenClose() {
			// TODO instantiate the class, 
			// raise with init
			// connect
			// close
			// check what happens
			// reopen

			//finally close

		}







		public List<String> getModifiedEntries(Timestamp ts) {
			// TODO Auto-generated method stub
			return null;
		}


		public List<String> getNewEntries(Timestamp ts) {
			// TODO Auto-generated method stub
			return null;
		}


		public List<String> testGetOneEntry(String uid) {
			// TODO Auto-generated method stub
			return null;
		}












	}
