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

This sample requires that you securely store the name and key of your storage account. Store them in environment variables local to the machine that runs the sample. Follow either the Linux or the Windows example, depending on your operating system, to create the environment variables. In Windows, the environment variable is not available until you reload the **Command Prompt** or shell you are using.

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

1.  To run the sample, use Maven at the command line.

2. open a shell and browse to **storage-blobs-java-v10-quickstart** inside your cloned directory.
3. Enter `mvn compile exec:java`.

This sample creates a test file in your default directory, **AppData\Local\Temp**, for Windows users. Then it prompts you to take the following steps:

1. Enter commands to upload the test file to Azure Blob storage.
2. List the blobs in the container.
3. Download the uploaded file with a new name so you can compare the old and new files.
4. Close the sample, which will also clean up any resources the sample created.

You can create an invalid static route for all requests to the primary endpoint of your read-access geo-redundant (RA-GRS) storage account. In this sample, the local host is used as the gateway for routing requests to the storage account. Using the local host as the gateway causes all requests to your storage account primary endpoint to loop back inside the host, which subsequently leads to failure.

5. Run the java application and enter **P**, the application will upload a file and then pause for your next command.

With the application paused, start command prompt on Windows as an administrator or run terminal as root on Linux.

6. Get information about the storage account primary endpoint domain by entering the following command on a command prompt or terminal.

 Replace `STORAGEACCOUNTNAME` with the name of your storage account. Copy to the IP address of your storage account to a text editor for later use.

```
nslookup STORAGEACCOUNTNAME.blob.core.windows.net
```

7. Get the IP address of your local host, enter `ipconfig` on the Windows command prompt, or `ifconfig` on the Linux terminal.

8. Add a static route for a destination host, enter the following command on a Windows command prompt or Linux terminal.

 Replace  `<destination_ip>` with your storage account IP address, and `<gateway_ip>` with your local host IP address.

# [Linux](#tab/linux)

  route add <destination_ip> gw <gateway_ip>

# [Windows](#tab/windows)

  route add <destination_ip> <gateway_ip>

---

9. Return to your application and press **G** to initiate another download. In the output, you will see that the application has switched to the secondary pipeline.

10. Go back into your shell and enter `route delete <destination_IP>`.

11. Back in the application, enter **G** again. You will see it switch back to primary and run successfully against primary again.

If you run the application repeatedly, be sure the script change is commented out before you start the application.

## More information
- [About Azure storage accounts](https://docs.microsoft.com/azure/storage/storage-create-storage-account)
- [Designing HA Apps with RA-GRS storage](https://docs.microsoft.com/azure/storage/storage-designing-ha-apps-with-ra-grs/)
- [Azure Storage Replication](https://docs.microsoft.com/azure/storage/storage-redundancy)