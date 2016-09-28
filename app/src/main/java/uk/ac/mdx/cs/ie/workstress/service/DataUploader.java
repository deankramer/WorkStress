package uk.ac.mdx.cs.ie.workstress.service;

import java.util.ArrayList;
import java.util.List;

import uk.ac.mdx.cs.ie.workstress.utility.StressReport;

/**
 * Interface to handle communication with webservice for data collection
 *
 * @author Dean Kramer <d.kramer@mdx.ac.uk>
 */
public interface DataUploader {

    boolean ranOutOfTime(final Integer user);

    List getAllUsers();

    boolean uploadReports(final Integer user, final List<StressReport> reports);

    void uploadHeartBeats(final boolean resend, final Integer user, final ArrayList<Integer> heartbeats, final ArrayList<Long> timestamps);

    void closeConnection();
}
