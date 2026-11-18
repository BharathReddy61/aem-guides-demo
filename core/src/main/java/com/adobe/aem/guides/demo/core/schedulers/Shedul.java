// package com.adobe.aem.guides.demo.core.schedulers;

// import org.apache.sling.commons.scheduler.ScheduleOptions;
// import org.apache.sling.commons.scheduler.Scheduler;
// import org.osgi.service.component.annotations.Activate;
// import org.osgi.service.component.annotations.Component;
// import org.osgi.service.component.annotations.Reference;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// @Component(service = Runnable.class, immediate = true)
// public class Shedul implements Runnable {

//     // 1. Corrected SLF4J imports and class referencing
//     private static final Logger LOGGER = LoggerFactory.getLogger(Shedul.class);

//     @Reference
//     private Scheduler scheduler;

//     private static final String JOB_NAME = "My Scheduler Job";

//     @Activate
//     protected void activate() {
//         // 2. Fixed lowercase method name .expr()
//         ScheduleOptions options = scheduler.EXPR("0/30 * * * * ?"); 
//         options.name(JOB_NAME);
//         options.canRunConcurrently(false);
        
//         scheduler.schedule(this, options);
//         LOGGER.info("Scheduler activated and job scheduled successfully.");
//     }

//     @Override
//     public void run() {
//         LOGGER.info("Scheduler is running...");
//     }
// }