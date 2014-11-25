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

/**
 * Support for testing the dcFileSever demo.  This shows the DivConq remote API
 * system support. 
 */
package divconq.interchange.simple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import divconq.api.ApiSession;
import divconq.api.DumpCallback;
import divconq.api.ServiceResult;
import divconq.api.tasks.TaskFactory;
import divconq.bus.Message;
import divconq.ctp.f.CtpFClient;
import divconq.filestore.CommonPath;
import divconq.hub.Foreground;
import divconq.hub.Hub;
import divconq.hub.ILocalCommandLine;
import divconq.lang.TimeoutPlan;
import divconq.lang.op.FuncResult;
import divconq.lang.op.OperationContext;
import divconq.lang.op.OperationObserver;
import divconq.lang.op.OperationResult;
import divconq.log.DebugLevel;
import divconq.script.Activity;
import divconq.script.ui.ScriptUtility;
import divconq.struct.FieldStruct;
import divconq.struct.ListStruct;
import divconq.struct.RecordStruct;
import divconq.struct.Struct;
import divconq.util.FileUtil;
import divconq.util.IOUtil;
import divconq.util.StringUtil;
import divconq.work.Task;
import divconq.work.TaskRun;
import divconq.xml.XElement;

public class FileStoreClient implements ILocalCommandLine {
	@Override
	public void run(final Scanner scan, final ApiSession api) {
		boolean running = true;
		
		String fsService = "dcTestFileServer";		
		
		XElement cliset = Hub.instance.getConfig().selectFirst("CommandLine/Settings");
		
		if (cliset != null) {
			fsService = cliset.getAttribute("FSServiceName", fsService);
		}
		
		final String finfsService = fsService;		
		
		while(running) {
			try {
				System.out.println();
				System.out.println("-----------------------------------------------");
				System.out.println("   Simple File Store Client");
				System.out.println("-----------------------------------------------");
				System.out.println("0)  Exit");
				System.out.println("1)  List Files");
				System.out.println("2)  Upload File");
				System.out.println("3)  Download File");
				System.out.println("4)  Delete File");
				System.out.println("5)  Make Folder");
				System.out.println("6)  Delete Folder");
				System.out.println("7)  File Details");
				System.out.println("10) Generate Test Files");
				System.out.println("11) Test Uploads");
				System.out.println("12) Test Downloads");
				System.out.println("100) dcScript GUI Debugger");
				System.out.println("101) dcScript Run Script");
				//System.out.println("102) Test Throttle and Quick Resume");
				//System.out.println("104) Start Test dcBus");
				//System.out.println("105) Send Test dcBus");
				System.out.println("200) Local Utilities");
				System.out.println("201) Ctp Client");

				String opt = scan.nextLine();
				
				Long mopt = StringUtil.parseInt(opt);
				
				if (mopt == null)
					continue;
				
				switch (mopt.intValue()) {
				case 0: {
					running = false;
					break;
				}
				
				case 1: {
					System.out.println("---------- List Files ----------");
					
					System.out.println("Server Path ([enter] for root): ");
					String spath = scan.nextLine();
			    	
			    	if (StringUtil.isEmpty(spath))
			    		spath = "/";
			    	
			    	Message msg = new Message(fsService, "FileStore", "ListFiles", new RecordStruct(
			    			new FieldStruct("FolderPath", spath)
			    	));
			    	
			    	api.sendMessage(msg, new ServiceResult(TimeoutPlan.Long) {						
						@Override
						public void callback() {
							if (this.hasErrors()) {
								System.out.println("Error listing files: " + this.getCode() + " - " + this.getMessage());
							}
							else {
								this.getResult().getFieldAsList("Body").recordStream().forEach(rec -> {
									System.out.println(
											rec.getFieldAsString("FileName") 
											+ " " + rec.getFieldAsString("Size")
											+ " " + rec.getFieldAsString("LastModified")
											+ " " + (rec.getFieldAsBoolean("IsFolder") ? "FOLDER" : "")
									);
								});
							}
						}
					});
			    	
			    	
					break;
				}
				
				case 2: {
					System.out.println("---------- Upload File ----------");
					
					System.out.println("Local File Name: ");
					String fname = scan.nextLine();		
					
					System.out.println("Save to Folder ([enter] for root): ");
					String spath = scan.nextLine();
			    	
			    	Path src = Paths.get(fname);
			    	CommonPath dest = new CommonPath(spath + "/" + src.getFileName());
			    	
			    	Task uploadtask = TaskFactory.createUploadTask(api, fsService, src, dest, null, true);
			    	
			    	Hub.instance.getWorkPool().submit(uploadtask, new OperationObserver() {
						@Override
						public void completed(OperationContext or) {
							if (or.hasErrors())
								System.out.println("Upload failed!");
							else
								System.out.println("Upload worked!");
						}
					});
			    	
					break;
				}
				
				case 3: {
					System.out.println("---------- Download File ----------");
					
					System.out.println("File Name: ");
					final String spath = scan.nextLine();
					
			    	final CommonPath src = new CommonPath(spath);
					
					System.out.println("Local Save Path: ");
					final Path dest = Paths.get(scan.nextLine(), src.getFileName());
			    	
			    	Task downloadtask = TaskFactory.createDownloadTask(api, fsService, dest, src, null, true);
			    	
			    	Hub.instance.getWorkPool().submit(downloadtask, new OperationObserver() {
						@Override
						public void completed(OperationContext or) {
							if (or.hasErrors())
								System.out.println("Download failed!");
							else
								System.out.println("Download worked!");
						}
					});
					
					break;
				}
				
				case 4: {
					System.out.println("---------- Delete File ----------");
					System.out.println("File Path: ");
					String spath = scan.nextLine();
			    	
			    	Message msg = new Message(fsService, "FileStore", "DeleteFile", new RecordStruct(
			    			new FieldStruct("FilePath", spath)
			    	));
			    	
			    	api.sendMessage(msg, new DumpCallback("Delete Result"));			    	
			    	
					break;
				}
				
				case 5: {
					System.out.println("---------- Make Folder ----------");
					System.out.println("Folder Path: ");
					String spath = scan.nextLine();
			    	
			    	Message msg = new Message(fsService, "FileStore", "AddFolder", new RecordStruct(
			    			new FieldStruct("FolderPath", spath)
			    	));
			    	
			    	api.sendMessage(msg, new DumpCallback("Make Result"));			    	
			    	
					break;
				}
				
				case 6: {
					System.out.println("---------- Delete Folder ----------");
					System.out.println("Folder Path: ");
					String spath = scan.nextLine();
			    	
			    	Message msg = new Message(fsService, "FileStore", "DeleteFolder", new RecordStruct(
			    			new FieldStruct("FolderPath", spath)
			    	));
			    	
			    	api.sendMessage(msg, new DumpCallback("Delete Result"));			    	
			    	
					break;
				}
				
				case 7: {
					System.out.println("---------- File Details ----------");
					System.out.println("File Path: ");
					String spath = scan.nextLine();
					
					System.out.println("Method (MD5, SHA128, SHA256 or empty for no hash): ");
					String meth = scan.nextLine();
			    	
			    	Message msg = new Message(fsService, "FileStore", "FileDetail", new RecordStruct(
			    			new FieldStruct("FilePath", spath),
			    			new FieldStruct("Method", StringUtil.isNotEmpty(meth) ? meth : null)
			    	));
			    	
			    	api.sendMessage(msg, new DumpCallback("Detail Result"));			    	
			    	
					break;
				}
				
				case 10: {
					System.out.println("Generate in Folder (path): ");
					String spath = scan.nextLine();
			    	
					Path genfldr = Paths.get(spath);
					
					System.out.println("Generate files");
					
					System.out.println("Number to create: ");
					int num = (int) StringUtil.parseInt(scan.nextLine(), 0);
					
					System.out.println("Min size: ");
					int minsize = (int) StringUtil.parseInt(scan.nextLine(), 0);
					
					System.out.println("Max size: ");
					int maxsize = (int) StringUtil.parseInt(scan.nextLine(), 0);

					for (int run = 0; run < num; run++) {
						final Path testfile = FileUtil.generateTestFile(genfldr, "bin", minsize, maxsize);
						
						System.out.println("File: " + testfile.toString());
					}

					break;
				}	// end case 10
				case 11: {
					System.out.println("Uploads to run serially: ");
					final int runs = (int) StringUtil.parseInt(scan.nextLine(), 0);
					
					System.out.println("Upload from Folder (path): ");
					String spath = scan.nextLine();
			    	
					Path genfldr = Paths.get(spath);
					
					System.out.println("Upload to Folder (path): ");
					String dpath = scan.nextLine();
					
			    	final CommonPath dest = new CommonPath(dpath);
					
					System.out.println("Test resume (y/n): ");
					final boolean resumetest = (scan.nextLine().toLowerCase().startsWith("y"));
					
					final AtomicInteger runsleft = new AtomicInteger(runs);
					final AtomicInteger successcnt = new AtomicInteger();
					final AtomicLong successamt = new AtomicLong();
					final AtomicInteger failcnt = new AtomicInteger();
					final AtomicInteger abortcnt = new AtomicInteger();
					final AtomicReference<Runnable> runupload = new AtomicReference<>();
					
					final AtomicReference<Path> resumepath = new AtomicReference<>();
					final AtomicBoolean resumeneeded = new AtomicBoolean();
					
					final long start = System.currentTimeMillis();
					
					final Path[] genfiles = Files.list(genfldr).toArray(Path[]::new);

					runupload.set(new Runnable() {						
						@Override
						public void run() {
							if (runsleft.get() <= 0) {
								System.out.println();
								System.out.println("     Runs: " + runs);
								System.out.println("Successes: " + successcnt.get());
								System.out.println(" Failures: " + failcnt.get());
								System.out.println("   Aborts: " + abortcnt.get());
								System.out.println("     Time: " + ((System.currentTimeMillis() - start) / 1000));
								System.out.println("     Data: " + successamt.get());
								System.out.println();
								return;
							}
							
							runsleft.decrementAndGet();
							
							final boolean resume = resumeneeded.get();									
							
							// grab a file randomly or last file used if resume
							final Path local = resume ? resumepath.get() : genfiles[FileUtil.testrnd.nextInt(genfiles.length)];
							
							// next run upload run is not a resume (yet)
							resumeneeded.set(false);
							resumepath.set(local);
							
							Task uploadtask = TaskFactory.createUploadTask(api, finfsService, local, dest.resolve(local.getFileName().toString()), null, resume);
					    	
							TaskRun run = new TaskRun(uploadtask);

							uploadtask.withObserver(new OperationObserver() {
					    		protected boolean failTry1 = false;
					    		protected boolean failTry2 = false;
					    		
								@Override
								public void completed(OperationContext or) {
									if (or.hasErrors()) {
										failcnt.incrementAndGet();
										System.out.println("Upload failed: " + local);
									}
									else {
										successcnt.incrementAndGet();
										System.out.println("Upload worked: " + local);
										
										try {
											successamt.addAndGet(Files.size(local));
										} 
										catch (IOException x) {
											System.out.println("+++++++++++++++++++++++++++ Issue with collecting successful file upload size");
										}
									}
									
									runupload.get().run();
								}
					    		
								@Override
					    		public void step(OperationContext or, int num, int of, String name) {
									System.out.println("Step: " + num + "/" + of + " - " + name);
					    		}
					    		
								@Override
								public void progress(OperationContext or, String msg) {
									System.out.println("Progress: " + msg);
								}

								@Override
								public void amount(OperationContext or, int v) {
									System.out.println("Amount: " + run.getContext().getAmountCompleted());
									
									// if we are streaming try 2 times to abort
									if ((run.getContext().getCurrentStep() == 2) && resumetest) {
										if (!this.failTry1 && (run.getContext().getAmountCompleted() > 40) &&  (run.getContext().getAmountCompleted() < 42)) {
											// 25% chance of a failure
											if (FileUtil.testrnd.nextInt(4) == 0) {
												System.out.println("attempting to abort stream ##################################");
												
												resumeneeded.set(true);
												runsleft.incrementAndGet();
												abortcnt.incrementAndGet();
												api.abortStream(run.getTask().getParams().getFieldAsRecord("StreamInfo").getFieldAsString("ChannelId"));
											}
											
											this.failTry1 = true;
										}
										
										if (!this.failTry2 && (run.getContext().getAmountCompleted() > 80) &&  (run.getContext().getAmountCompleted() < 82)) {
											// 25% chance of a failure
											if (FileUtil.testrnd.nextInt(4) == 0) {
												System.out.println("attempting to abort stream ##################################");
												
												resumeneeded.set(true);
												runsleft.incrementAndGet();
												abortcnt.incrementAndGet();
												api.abortStream(run.getTask().getParams().getFieldAsRecord("StreamInfo").getFieldAsString("ChannelId"));
											}
											
											this.failTry2 = true;
										}
									}
								}
							});
							
					    	Hub.instance.getWorkPool().submit(run);
						}
					});
					
					runupload.get().run();

					break;
				}	// end case 11
				case 12: {
					System.out.println("Downloads to run serially: ");
					final int runs = (int) StringUtil.parseInt(scan.nextLine(), 0);
					
					System.out.println("Download from Folder (path): ");
					String spath = scan.nextLine();
					
			    	final CommonPath src = new CommonPath(spath);
					
					System.out.println("Download to Folder (path): ");
					String dpath = scan.nextLine();
			    	
					Path dest = Paths.get(dpath);
					
					System.out.println("Test resume (y/n): ");
					final boolean resumetest = (scan.nextLine().toLowerCase().startsWith("y"));
					
					final AtomicInteger runsleft = new AtomicInteger(runs);
					final AtomicInteger successcnt = new AtomicInteger();
					final AtomicLong successamt = new AtomicLong();
					final AtomicInteger failcnt = new AtomicInteger();
					final AtomicInteger abortcnt = new AtomicInteger();
					final AtomicReference<Runnable> runupload = new AtomicReference<>();
					
					final AtomicReference<CommonPath> resumepath = new AtomicReference<>();
					final AtomicBoolean resumeneeded = new AtomicBoolean();
					
					final long start = System.currentTimeMillis();
								    	
			    	Message msg = new Message(fsService, "FileStore", "ListFiles", new RecordStruct(
			    			new FieldStruct("FolderPath", src)
			    	));
			    	
			    	api.sendMessage(msg, new ServiceResult(TimeoutPlan.Long) {						
						@Override
						public void callback() {
							if (this.hasErrors()) {
								System.out.println("Error listing files: " + this.getCode() + " - " + this.getMessage());
							}
							else {
								final CommonPath[] genfiles = this.getResult().getFieldAsList("Body").recordStream()
										.map(rec -> src.resolve(rec.getFieldAsString("FileName")))
										.toArray(CommonPath[]::new);
			
								runupload.set(new Runnable() {						
									@Override
									public void run() {
										if (runsleft.get() <= 0) {
											System.out.println();
											System.out.println("     Runs: " + runs);
											System.out.println("Successes: " + successcnt.get());
											System.out.println(" Failures: " + failcnt.get());
											System.out.println("   Aborts: " + abortcnt.get());
											System.out.println("     Time: " + ((System.currentTimeMillis() - start) / 1000));
											System.out.println("     Data: " + successamt.get());
											System.out.println();
											return;
										}
										
										runsleft.decrementAndGet();
										
										final boolean resume = resumeneeded.get();									
										
										// grab a file randomly or last file used if resume
										final CommonPath remote = resume ? resumepath.get() : genfiles[FileUtil.testrnd.nextInt(genfiles.length)];
										
										// next run upload run is not a resume (yet)
										resumeneeded.set(false);
										resumepath.set(remote);
										
										final Path local = dest.resolve(remote.getFileName());
										
										Task downloadtask = TaskFactory.createDownloadTask(api, finfsService, local, remote, null, resume);
								    	
										TaskRun run = new TaskRun(downloadtask);
			
										downloadtask.withObserver(new OperationObserver() {
								    		protected boolean failTry1 = false;
								    		protected boolean failTry2 = false;
								    		
											@Override
											public void completed(OperationContext or) {
												if (or.hasErrors()) {
													failcnt.incrementAndGet();
													System.out.println("Download failed: " + remote);
												}
												else {
													successcnt.incrementAndGet();
													System.out.println("Download worked: " + remote);
													
													try {
														successamt.addAndGet(Files.size(local));
													} 
													catch (IOException x) {
														System.out.println("+++++++++++++++++++++++++++ Issue with collecting successful file download size");
													}
												}
												
												runupload.get().run();
											}
								    		
											@Override
								    		public void step(OperationContext or, int num, int of, String name) {
												System.out.println("Step: " + num + "/" + of + " - " + name);
								    		}
								    		
											@Override
											public void progress(OperationContext or, String msg) {
												System.out.println("Progress: " + msg);
											}
			
											@Override
											public void amount(OperationContext or, int v) {
												System.out.println("Amount: " + run.getContext().getAmountCompleted());
												
												// if we are streaming try 2 times to abort
												if ((run.getContext().getCurrentStep() == 2) && resumetest) {
													if (!this.failTry1 && (run.getContext().getAmountCompleted() > 40) &&  (run.getContext().getAmountCompleted() < 42)) {
														// 25% chance of a failure
														if (FileUtil.testrnd.nextInt(4) == 0) {
															System.out.println("attempting to abort stream ##################################");
															
															resumeneeded.set(true);
															runsleft.incrementAndGet();
															abortcnt.incrementAndGet();
															api.abortStream(run.getTask().getParams().getFieldAsRecord("StreamInfo").getFieldAsString("ChannelId"));
														}
														
														this.failTry1 = true;
													}
													
													if (!this.failTry2 && (run.getContext().getAmountCompleted() > 80) &&  (run.getContext().getAmountCompleted() < 82)) {
														// 25% chance of a failure
														if (FileUtil.testrnd.nextInt(4) == 0) {
															System.out.println("attempting to abort stream ##################################");
															
															resumeneeded.set(true);
															runsleft.incrementAndGet();
															abortcnt.incrementAndGet();
															api.abortStream(run.getTask().getParams().getFieldAsRecord("StreamInfo").getFieldAsString("ChannelId"));
														}
														
														this.failTry2 = true;
													}
												}
											}
										});
										
								    	Hub.instance.getWorkPool().submit(run);
									}
								});
								
								runupload.get().run();
							}
						}
					});

					break;
				}	// end case 12
				
				case 100: {
					ScriptUtility.goSwing(null);					
					break;
				}
				
				case 101: {
					System.out.println("*** Run A dcScript ***");
					System.out.println("If you are looking for something to try, consider one of these:");
					System.out.println("  ./packages/dcTest/dcs/examples/99-bottles.dcs.xml");
					System.out.println("  ./packages/dcTest/dcs/examples/99-bottles-debug.dcs.xml");
					
					System.out.println();
					System.out.println("Path to script to run: ");
					String spath = scan.nextLine();
			    	
					System.out.println();
					
					FuncResult<CharSequence> rres = IOUtil.readEntireFile(Paths.get(spath));
					
					if (rres.hasErrors()) {
						System.out.println("Error reading script: " + rres.getMessage());
						break;
					}
					
					Activity act = new Activity();
					
					OperationResult compilelog = act.compile(rres.getResult().toString());
					
					if (compilelog.hasErrors()) {
						System.out.println("Error compiling script: " + compilelog.getMessage());
						break;
					}
					
					Task task = new Task()
						.withRootContext()
						.withTitle(act.getScript().getXml().getAttribute("Title", "Debugging dcScript"))	
						.withTimeout(0)							// no timeout in editor mode
						.withWork(act);
					
					Hub.instance.getWorkPool().submit(task);
					
					break;
				}
				
				/*
				case 102: {
					IWork w = new IWork() {
						int x = 0;
						
						@Override
						public void run(TaskRun trun) {
							x++;
							
							if (x % 1000 == 0)
								trun.info("msg: " + x);
							
							if (x == 100000)
								trun.complete();
							else
								trun.resume();
						}
					};
					
					Task task = new Task()
						.withRootContext()
						.withTitle("Debugging resume work")	
						.withTimeout(0)							// no timeout in editor mode
						.withThrottle(1)
						.withWork(w);
					
					Hub.instance.getWorkPool().submit(task);
					
					break;
				}
				
				case 103: {
					Task task = new Task()
						.withTitle("Many Bottles")
						.withThrottle(10)
						.withRootContext();
					
					if (!ScriptWork.addScript(task, Paths.get("./packages/dcTest/dcs/many-bottles.dcs.xml"))) {
						System.out.println("Error compiling script");
						break;
					}
					
					Hub.instance.getWorkPool().submit(task, new TaskObserver() {
						@Override
						public void completed(TaskRun or) {
							System.out.println("Script done. #" 
									+ ((Activity)or.getTask().getWorkInstance()).getRuntime() 
									+ " - Cnt: " + ((Activity)or.getTask().getWorkInstance()).getRunCount() 
									+ " - Code: " + or.getCode() 
									+ " - Message: " + or.getMessage());			
						}
					});
					
					break;
				}
				
				case 104: {
					LocalEcho.start();
					
					break;
				}
				
				case 105: {
					LocalEcho.test1();
					
					break;
				}
				
				case 106: {
					LocalEcho.test2();
					
					break;
				}
				
				case 107: {
					TarArchiveInputStream tarin = new TarArchiveInputStream(new FileInputStream("c:/temp/test/files.tar"));
					
					TarArchiveEntry entry = tarin.getNextTarEntry();
					
					while (entry != null) {
						System.out.println("name: " + entry.getName());
						
						entry = tarin.getNextTarEntry();
					}
					
					tarin.close();
					
					break;
				}
				
				case 108: {
					
					long x = StringUtil.parseLeadingInt("10MB");
					
					System.out.println("1: " + x);
					
					x = StringUtil.parseLeadingInt("10 MB");
					
					System.out.println("2: " + x);
					
					x = StringUtil.parseLeadingInt("1024");
					
					System.out.println("3: " + x);
					
					break;
				}
				
				case 109: {
					//PGPUtil.encryptFile("/Work/Temp/Dest/karabiner2.bin.gpg", "/Work/Temp/Dest/karabiner.bin", "/Users/Owner/.gnupg/pubring.gpg");
					PGPUtil.encryptFile("/Work/Temp/Dest/long score 2.txt.gpg", "/Work/Temp/Dest/long score.txt", "/Users/Owner/.gnupg/pubring.gpg");
					
					break;
				}
				*/
				
				case 200: {
					Foreground.utilityMenu(scan);					
					break;
				}
				
				case 201: {
					CtpFClient.utilityMenu(scan);
					break;
				}
				
				/*
				case 202: {
					//System.out.println("Value to set: ");
					//String spath = scan.nextLine();
					
					  // a static method that loads the RocksDB C++ library.
					  RocksDB.loadLibrary();
					  // the Options class contains a set of configurable DB options
					  // that determines the behavior of a database.
					  Options options = new Options().setCreateIfMissing(true);
					  RocksDB db = null;
					  
					  try {
					    // a factory method that returns a RocksDB instance
					    db = RocksDB.open(options, "/Work/Temp/rocks");
					    // do something
					    
					    /*
					    byte[] k1 = Utf8Encoder.encode("T1");
					    
					    byte[] value = db.get(k1);
					    
					    if (value != null) {  // value == null if key1 does not exist in db.
					    	System.out.println("Found value: " + Utf8Decoder.decode(value));
					    }					    
					    else
					    	System.out.println("No value found");
					    
					      db.put(k1, Utf8Encoder.encode(spath));
					    * /

					    System.out.println("Forwards: ");
					    
					    RocksIterator it = db.newIterator();
					    
					    for (it.seekToFirst(); it.isValid(); it.next()) {
						    byte[] key = it.key();
					    	System.out.println("Found key: " + HexUtil.bufferToHex(key));
					    }

					    System.out.println();
					    System.out.println("Backwards: ");
					    
					    it = db.newIterator();
					    
					    for (it.seekToLast(); it.isValid(); it.prev()) {
						    byte[] key = it.key();
					    	System.out.println("Found key: " + HexUtil.bufferToHex(key));
					    }
					    
					    /*
					    byte[] a = new byte[] { (byte) 0x01 };		// false
					    byte[] b = new byte[] { (byte) 0x10, (byte) 0x22, (byte) 0x22, (byte) 0x22 };		// pat
					    byte[] c = new byte[] { (byte) 0x10, (byte) 0x55, (byte) 0x55, (byte) 0x55 };		// mike
					    byte[] d = new byte[] { (byte) 0x10, (byte) 0x66 };		// stan
					    byte[] omega = new byte[] { (byte) 0xff };
					    
					    db.put(a, a);
					    db.put(b, Utf8Encoder.encode("pat"));
					    db.put(c, Utf8Encoder.encode("mike"));
					    db.put(d, Utf8Encoder.encode("stan"));
					    db.put(omega, omega);
					    
					    it = db.newIterator();
					    
					    // seek last string
					    byte[] ls = new byte[] { (byte) 0x11 };		// past strings
					    it.seek(ls);
					    
					    System.out.println("is valid 1: " + it.isValid());
					    
					    byte[] key = it.key();
					    
					    if (key != null) {  
					    	System.out.println("Found key: " + HexUtil.bufferToHex(key));
					    }					    
					    else
					    	System.out.println("No key found");
					    
					    it.prev();
					    
					    System.out.println("is valid 2: " + it.isValid());
					    
					    key = it.key();
					    
					    if (key != null) {  
					    	System.out.println("Found key: " + HexUtil.bufferToHex(key));
					    }					    
					    else
					    	System.out.println("No key found");
					    * /
					    
					    it = db.newIterator();
					    
					    // HOW TO SEEK PAST SOMETHING and then 1 back to find it
					    // seek past pat
					    byte[] ls = new byte[] { (byte) 0x10, (byte) 0x22, (byte) 0x22, (byte) 0x22, (byte) 0x00 };		// past pat
					    it.seek(ls);
					    
					    System.out.println("is valid 1: " + it.isValid());
					    
					    byte[] key = it.key();
					    
					    if (key != null) {  
					    	System.out.println("Found key: " + HexUtil.bufferToHex(key));
					    }					    
					    else
					    	System.out.println("No key found");
					    
					    it.prev();
					    
					    System.out.println("is valid 2: " + it.isValid());
					    
					    key = it.key();
					    
					    if (key != null) {  
					    	System.out.println("Found key: " + HexUtil.bufferToHex(key));
					    }					    
					    else
					    	System.out.println("No key found");
					    
					    //db.put(arg0, arg1);
					      
					     /*
    auto iter = DB::NewIterator(ReadOptions());
    for (iter.Seek(prefix); iter.Valid() && iter.key().startswith(prefix); iter.Next()) {
       // do something
    }					      * 
					      * /
					      
					  } 
					  catch (RocksDBException x) {
					    // do some error handling
					    System.out.println("rocks error!!");
					  }
					  finally {
						  if (db != null) db.close();
						  options.dispose();
					  }
					  
					  break;
				}
				*/
				
				
				case 203: {
					OperationContext ctx = OperationContext.allocateGuest();
					
					ctx.setLevel(DebugLevel.Trace);
					
					ctx.setLimitLog(false);
					
					OperationContext.set(ctx);
					
					System.out.println("Test Setup");
					
					for (int i = 0; i < 5; i++)
						ctx.info("Entry #" + i);
					
					OperationResult or = new OperationResult();
					
					or.debug("invisible 1");
					or.trace("invisible a");
					
					or.info("OR 1");
					or.info("OR 2");
					or.info("OR 3");
					
					or.markEnd();
					
					or.debug("invisible 2");
					
					for (int i = 5; i < 10; i++)
						ctx.info("Entry #" + i);
					
					System.out.println();
					System.out.println("What is in OC:");
					
					ListStruct msgs = ctx.getMessages();
					
					for (Struct s : msgs.getItems())
						System.out.println("- " + s);
					
					System.out.println();
					System.out.println("What is in OR:");
					
					msgs = or.getMessages();
					
					for (Struct s : msgs.getItems())
						System.out.println("- " + s);
					
					break;
				}
				}
			}
			catch (Exception x) {
				System.out.println("Command Line Error: " + x);
			}
		}
	}
}
