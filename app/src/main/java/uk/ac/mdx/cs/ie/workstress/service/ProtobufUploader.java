/*Copyright 2016 WorkStress Experiment
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.ac.mdx.cs.ie.workstress.service;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import uk.ac.mdx.cs.ie.workstress.proto.AllUsersRequest;
import uk.ac.mdx.cs.ie.workstress.proto.AllUsersResponse;
import uk.ac.mdx.cs.ie.workstress.proto.HeartRatesRequest;
import uk.ac.mdx.cs.ie.workstress.proto.RanOutOfTimeRequest;
import uk.ac.mdx.cs.ie.workstress.proto.ServiceResponse;
import uk.ac.mdx.cs.ie.workstress.proto.StressReportsRequest;
import uk.ac.mdx.cs.ie.workstress.proto.UserInformation;
import uk.ac.mdx.cs.ie.workstress.proto.WorkStressServiceGrpc;
import uk.ac.mdx.cs.ie.workstress.utility.ExplicitIntentGenerator;
import uk.ac.mdx.cs.ie.workstress.utility.StressReport;
import uk.ac.mdx.cs.ie.workstress.utility.WorkstressUser;

/**
 * Class to handle communication with webservice for data collection using ProtoBuf
 *
 * @author Dean Kramer <d.kramer@mdx.ac.uk>
 */
public class ProtobufUploader implements DataUploader {

    private String SERVER_URL;
    private static final int SERVER_PORT = 8081;
    private String API_KEY;
    private static final String LOG_TAG = "ProtobufUploader";
    private DataCollector mCollector;
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private ManagedChannel mChannel;
    private WorkStressServiceGrpc.WorkStressServiceBlockingStub mStub;


    public ProtobufUploader(Context context, DataCollector collector) {
        mContext = context;
        mCollector = collector;
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        Bundle metadata = null;

        try {
            metadata = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        API_KEY = metadata.getString("workstressService_ApiKey", "");
        SERVER_URL = metadata.getString("workstressService_Host", "");

        mChannel = ManagedChannelBuilder.forAddress(SERVER_URL, SERVER_PORT)
                .usePlaintext(true)
                .build();

        mStub = WorkStressServiceGrpc.newBlockingStub(mChannel);
    }

    @Override
    public boolean ranOutOfTime(final String user) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    if (!isConnected()) {
                        return;
                    }
                } catch (Exception e) {
                    return;
                }

                RanOutOfTimeRequest message = RanOutOfTimeRequest.newBuilder()
                        .setApikey(API_KEY)
                        .setUser(user)
                        .build();


                ServiceResponse response = mStub.outoftime(message);
            }
        }).start();
        return true;
    }

    @Override
    public List getAllUsers() {

        final List users = new ArrayList();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    if (!isConnected()) {
                        return;
                    }
                } catch (Exception e) {
                    return;
                }

                AllUsersRequest message = AllUsersRequest.newBuilder()
                        .setApikey(API_KEY)
                        .build();

                AllUsersResponse response = mStub.getallusers(message);

                for (UserInformation user : response.getUsersList()) {
                    WorkstressUser wsu = new WorkstressUser();
                    wsu.userid = user.getUserid();
                    wsu.username = user.getUsername();
                    users.add(wsu);
                }
            }
        });

        t.start();

        try {
            t.join();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        return users;
    }

    @Override
    public boolean uploadReports(final String user, final List<StressReport> reports) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    if (!isConnected()) {
                        return;
                    }
                } catch (Exception e) {
                    return;
                }

                StressReportsRequest.Builder message = StressReportsRequest.newBuilder();

                message.setApikey(API_KEY);
                message.setUser(user);

                for (StressReport report : reports) {
                    uk.ac.mdx.cs.ie.workstress.proto.StressReport.Builder reportMessage =
                            uk.ac.mdx.cs.ie.workstress.proto.StressReport.newBuilder()
                                    .setQ1(report.question1)
                                    .setQ2(report.question2)
                                    .setQ3(report.question3)
                                    .setQ4(report.question4)
                                    .setQ5(report.question5)
                                    .setQ6(report.question6)
                                    .setQ7(report.question7)
                                    .setTimestamp(report.date);
                    message.addReports(reportMessage.build());
                }

                ServiceResponse response = mStub.newReports(message.build());

                if (response.getResponse() > -1) {
                    mCollector.completeOutstandingReports();
                }

            }
        }).start();

        return false;
    }

    @Override
    public void uploadHeartBeats(final boolean resend, final String user, final ArrayList<Integer> heartbeats, final ArrayList<Long> timestamps) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    if (!isConnected()) {
                        return;
                    }
                } catch (Exception e) {
                    return;
                }

                HeartRatesRequest.Builder message = HeartRatesRequest.newBuilder();

                message.setApikey(API_KEY);
                message.setUser(user);
                message.addAllHeartrates(heartbeats);
                message.addAllTimestamps(timestamps);

                ServiceResponse response = mStub.newheartrates(message.build());

                int status = response.getResponse();

                if (status > -1) {
                    if (resend) {

                        mCollector.completeOutstandingRates();
                    } else {
                        mCollector.completeRates();
                    }
                } else {
                    mCollector.persistLog(heartbeats, timestamps);
                }

                if (status > 0) {
                    mCollector.needReport(status, response.getRequesttime());
                }

            }
        }).start();
    }

    @Override
    public void closeConnection() {
        try {
            mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public boolean isConnected() throws InterruptedException, IOException {
        String command = "ping -c 1 google.com";
        return (Runtime.getRuntime().exec(command).waitFor() == 0);
    }

}
