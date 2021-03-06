/* ************************************************************************
#
#  DivConq
#
#  http://divconq.com/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package divconq.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import divconq.filestore.CommonPath;
import divconq.hub.DomainInfo;
import divconq.hub.Hub;
import divconq.io.FileStoreEvent;
import divconq.io.LocalFileStore;
import divconq.lang.op.FuncCallback;
import divconq.mod.Bundle;
import divconq.mod.ExtensionLoader;
import divconq.mod.IExtension;
import divconq.net.IpAddress;
import divconq.util.MimeUtil;
import divconq.util.StringUtil;
import divconq.xml.XElement;

public class WebSiteManager {
	protected WebModule module = null;
	protected Map<String,IWebExtension> extensions = new HashMap<String,IWebExtension>();
	protected String defaultExtension = null;
	
	protected ConcurrentHashMap<String, IWebDomain> dsitemap = new ConcurrentHashMap<String, IWebDomain>();
	
	protected String version = null;
	
	// TODO when module is unloaded, clean up all references to classes

	protected Map<String,IWebMacro> macros = new HashMap<String,IWebMacro>();
	protected ValuesMacro vmacros = new ValuesMacro();
	
	protected List<XElement> devices = new ArrayList<XElement>();
	
	public Collection<IWebExtension> getSites() {
		return this.extensions.values();
	}
	
	public WebModule getModule() {
		return this.module;
	}
	
	public String getVersion() {
		return this.version;
	}
	
	public void start(WebModule module, XElement config) {
		this.module = module;

		if (config != null) {
			// ideally we would only load in Hub level settings, try to use sparingly
			MimeUtil.load(config);
			
			this.devices = config.selectAll("DeviceRule");
			
			for (XElement macros : config.selectAll("Macro")) {
				String name = macros.getAttribute("Name");
				
				if (StringUtil.isEmpty(name))
					continue;
				
				String bname = macros.getAttribute("Class");
				
				if (StringUtil.isNotEmpty(bname)) {
					Class<?> cls = this.module.getLoader().getClass(bname);
					
					if (cls != null) {
						Class<? extends IWebMacro> tcls = cls.asSubclass(IWebMacro.class);
						
						if (tcls != null)
							try {
								this.macros.put(name, tcls.newInstance());
							} 
							catch (Exception x) {
								x.printStackTrace();
							}
					} 

					// TODO log
					//System.out.println("unable to load class: " + cname);
				}
				
				String value = macros.getAttribute("Value");
				
				if (StringUtil.isNotEmpty(value)) 
					this.vmacros.add(name, value);
			}
		}
	
		// prepare extensions (web apps)
	
		XElement lcf = module.getLoader().getConfig();
		/* TODO try to recreate this concept
		Adler32 ad = new Adler32();
		*/
		
		if (lcf != null)
	    	for(XElement node : lcf.selectAll("Extension")) {
	    		String name = node.getAttribute("Name");
	    		
	    		Bundle bundle = this.module.getLoader().getExtension(name);
	    		
	    		if (! (bundle instanceof ExtensionLoader))
	    			return;

	    		IExtension ex = ((ExtensionLoader)bundle).getExtension();
	    		
	    		if (! (ex instanceof IWebExtension))
	    			return;
	    		
	    		// only compute if this is a web extension
	    		/* TODO try to recreate this concept
	    		bundle.adler(ad);
	    		*/
	    		
	    		IWebExtension sm = (IWebExtension)ex;

	    		this.extensions.put(name, sm);
	    		
	    		if (this.defaultExtension == null)
	    			this.defaultExtension = name;
	    	}
		
		/* TODO try to recreate this concept
		this.version = Long.toHexString(ad.getValue());
		*/
		
		// ========================================================================
		

		/**
		 * - ./private/dcw/filetransferconsulting/phantom/www/dcf/index.html
		 * - ./private/dcw/filetransferconsulting/static/www/dcf/index.html
		 * - ./public/dcw/filetransferconsulting/phantom/www/dcf/index.html
		 * - ./public/dcw/filetransferconsulting/static/www/dcf/index.html
		 */			

		FuncCallback<FileStoreEvent> localfilestorecallback = new FuncCallback<FileStoreEvent>() {
			@Override
			public void callback() {
				this.resetCalledFlag();
				
				CommonPath p = this.getResult().getPath();
				
				//System.out.println(p);
				
				// only notify on www updates
				if (p.getNameCount() < 5) 
					return;
				
				// must be inside a domain or we don't care
				String mod = p.getName(0);
				String domain = p.getName(1);
				String section = p.getName(3);
				
				if (!"dcw".equals(mod) || !"www".equals(section))
					return;
				
				for (IWebDomain wdomain : WebSiteManager.this.dsitemap.values()) {
					if (domain.equals(wdomain.getAlias())) {
						wdomain.siteNotify();
						break;
					}
				}
			}
		};
		
		// register for file store events
		LocalFileStore pubfs = Hub.instance.getPublicFileStore();
		
		if (pubfs != null) 
			pubfs.register(localfilestorecallback);
		
		LocalFileStore privfs = Hub.instance.getPrivateFileStore();
		
		if (privfs != null) 
			privfs.register(localfilestorecallback);

		/**
		 * - ./packages/zCustomPublic/www/dcf/index.html
		 * - ./packages/dc/dcFilePublic/www/dcf/index.html
		 * - ./packages/dcWeb/www/dcf/index.html
		 */			

		FuncCallback<FileStoreEvent> localpackagecallback = new FuncCallback<FileStoreEvent>() {
			@Override
			public void callback() {
				for (IWebDomain domain : WebSiteManager.this.dsitemap.values())
					domain.siteNotify();
				
				this.resetCalledFlag();
			}
		};
		
		// register for file store events
		LocalFileStore packfs = Hub.instance.getPackageFileStore();
		
		if (packfs != null) 
			packfs.register(localpackagecallback);
		
		/* TODO
		Hub.instance.listenOnline(new OperationCallback() {
			@Override
			public void callback() {
				// load dynamic web parts only when domains are loaded by Hub
				for (IWebExtension ext : WebSiteManager.this.extensions.values())
					ext.loadDynamic();
			}
		} );	
		*/	
	}
	
	public IWebDomain getDomain(String id) {
		DomainInfo di = Hub.instance.getDomainInfo(id);
		
		if (di != null) {
			IWebDomain domain = this.dsitemap.get(di.getId());
			
			if (domain != null)
				return domain; 
			
			for (DomainInfo d : Hub.instance.getDomains()) {
				if (d.getId().equals(id)) {
					domain = new WebDomain();
					domain.init(d);
					this.dsitemap.put(id, domain);
					
					return domain;
				}
			}
		}
		
		return null;
	}
	
	public void online() {
		this.dsitemap.clear();
	}	
	public String resolveHost(Request req) {
		return this.resolveHost(req.getHeader("Host"));
	}
	
	public String resolveHost(String dname) {
		if (StringUtil.isNotEmpty(dname)) {
			int cpos = dname.indexOf(':');
			
			if (cpos > -1)
				dname = dname.substring(0, cpos);
		}
		
		if (StringUtil.isEmpty(dname) || IpAddress.isIpLiteral(dname))
			dname = "localhost";
		
		return dname;
	}
	
	public DomainInfo resolveDomainInfo(Request req) {
		return Hub.instance.resolveDomainInfo(this.resolveHost(req));
	}
	
	public DomainInfo resolveDomainInfo(String dname) {
		return Hub.instance.resolveDomainInfo(this.resolveHost(dname));
	}

	public IWebMacro getMacro(String name) {
		if (this.vmacros.hasKey(name))
			return this.vmacros;
		
		return this.macros.get(name);
	}

	public IWebExtension getExtension(String name) {
		return this.extensions.get(name);
	}
	
	public IWebExtension getDefaultExtension() {
		return this.extensions.get(this.defaultExtension);
	}

	public String getDefaultName() {
		return this.defaultExtension;
	}
	
	public List<String> getDevice(Request req) {
		List<String> res = new ArrayList<String>();
		String agent = req.getHeader("User-Agent");
		
		if (StringUtil.isEmpty(agent)) {
			res.add("simple");
			res.add("std");
			res.add("mobile");
			
			return res;
		}
		
		for (XElement el : this.devices) {
			boolean fnd = (el.findIndex("AnyAgent") > -1);
			
			if (!fnd) {
				for (XElement fel : el.selectAll("IfAgent")) {
					if (agent.contains(fel.getAttribute("Contains"))) {
						fnd = true;
						break;
					}
				}
			}
			
			if (fnd) {
				for (XElement fel : el.selectAll("TryDevice")) {
					String name = fel.getAttribute("Name");
					
					if (StringUtil.isNotEmpty(name)) 
						res.add(name);
				}
				
				break;
			}
		}
		
		return res;
	}
	
}
