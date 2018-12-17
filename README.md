---
services: storage
platforms: java
author: roygara
---

# Using the Circuit Breaker pattern in your HA apps with RA-GRS Storage
This sample shows how to use the Circuit Breaker pattern with an RA-GRS storage account to switch your high-availability application to secondary storage when there is a problem with primary storage, and then switch back when primary storage becomes available again. For more information, please see [Designing HA Apps with RA-GRS storage](https://azure.microsoft.com/documentation/articles/storage-designing-ha-apps-with-ra-grs).

If you don't have a Microsoft Azure subscription, you can get a FREE trial account <a href="http://go.microsoft.com/fwlink/?LinkId=330212">here</a>.

## How it works

This sample creates a test file in your default directory, **AppData\Local\Temp**, for Windows users. Then the application uploads a file to a container in blob storage to use for the test. From there, you are in full control of the application. You may purposefully simulate a failure and this will demonstrate that if there is an error reading the primary, a retry is performed, and when your threshold has been exceeded, it will switch to secondary storage. 

In the case included here, the thresholds are arbitrary numbers for the count of allowable retries against the primary before switching to the secondary, and the count of allowable reads against the primary before switching to the secondary. You can use any algorithm to determine your thresholds; the purpose of this sample is just to show you how to capture the events and switch back and forth.

## How to run the sample

1.  If you don't already have it installed, download and install Fiddler on your [Windows](https://www.telerik.com/download/fiddler) or [Linux](http://telerik-fiddler.s3.amazonaws.com/fiddler/fiddler-linux.zip) machine. [More information](https://www.telerik.com/blogs/fiddler-for-linux-beta-is-here) on how to install Fiddler on Linux.

Fiddler will be used to modify the response from the service to indicate a failure, so it triggers the failover to secondary.

>NOTE
>Invalid static route can also be used to simulate failure.

2. This sample requires that you securely store the name and key of your storage account. Store them in environment variables local to the machine that runs the sample. Follow either the Linux or the Windows example, depending on your operating system, to create the environment variables. In Windows, the environment variable is not available until you reload the **Command Prompt** or shell you are using.

### Linux example

```
export AZURE_STORAGE_ACCOUNT="<youraccountname>"
export AZURE_STORAGE_ACCESS_KEY="<youraccountkey>"
```

### Windows example

```
setx AZURE_STORAGE_ACCOUNT "<youraccountname>"
setx AZURE_STORAGE_ACCESS_KEY "<youraccountkey>"
```


3. Run Fiddler or create an invalid static route.

4. Run the java application. It displays information on your console window showing the count of requests made against the storage service to download the file, and tells whether you are accessing the primary or secondary endpoint. You can also see the information in the Fiddler trace.

5. The application pauses after each command.

6. Go to Fiddler and select Rules > Customize Rules. Look for the OnBeforeResponse function and insert this code. (An example of the OnBeforeResponse method is included in the project in the Fiddler_script.txt file.)
```
	if ((oSession.hostname == "YOURSTORAGEACCOUNTNAME.blob.core.windows.net") 
	&& (oSession.PathAndQuery.Contains("HelloWorld"))) {
	   oSession.responseCode = 503;  
        }
```
	Change YOURSTORAGEACCOUNTNAME to your storage account name, and uncomment out this code. Save your changes to the script. 

7. Return to your application and press **G** to initiate another download. In the output, you will see the errors against primary that come from the intercept in Fiddler, and the switch to secondary storage. After the number of reads exceeds the threshold, you will see it switch back to primary. It does this repeatedly.

8. Go back into Fiddler and comment out the code and save the script. Continue running the application. You will see it switch back to primary and run successfully against primary again.

If you run the application repeatedly, be sure the script change is commented out before you start the application.


## More information
- [About Azure storage accounts](https://docs.microsoft.com/azure/storage/storage-create-storage-account)
- [Designing HA Apps with RA-GRS storage](https://docs.microsoft.com/azure/storage/storage-designing-ha-apps-with-ra-grs/)
- [Azure Storage Replication](https://docs.microsoft.com/azure/storage/storage-redundancy)