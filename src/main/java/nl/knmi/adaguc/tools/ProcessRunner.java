package nl.knmi.adaguc.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

public class ProcessRunner {
	Process child = null;
	int exitCode = 0;

	public interface StatusPrinterInterface {

		default void setPID(String pid) {
		};

		public void setError(String message);

		public String getError();

		public boolean hasData();

		public void print(byte[] message, int bytesRead);
	}

	// This threads runs during the execution of the child program
	private class StatusPrinterThread extends Thread {
		InputStream reader = null;

		StatusPrinterInterface printer = null;

		StatusPrinterThread(StatusPrinterInterface _printer, InputStream brstdout) {
			reader = brstdout;
			printer = _printer;
		}

		public void run() {
			try {
				int bytesRead = 0;
				byte[] cbuf = new byte[256];
				while ((bytesRead = reader.read(cbuf)) != -1) {
					printer.print(cbuf, bytesRead);
				}

			} catch (Exception e) {

				String msg = ("Exception in ProcessRunner.StatusPrinterThread: " + e.getMessage());
				printer.print(msg.getBytes(), msg.length());
			}
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
	}

	public StatusPrinterThread stdoutThread = null;
	public StatusPrinterThread stderrThread = null;
	public StatusPrinterInterface stdoutPrinter = null;
	public StatusPrinterInterface stderrPrinter = null;
	private String[] environmentVars = null;
	private String workingDirectory = null;
	private long timeOutMs = -1;

	public ProcessRunner(StatusPrinterInterface _stdoutPrinter, StatusPrinterInterface _stderrPrinter,
			String[] _environmentVars, String _workingDirectory, long _timeOutMs) {
		stdoutPrinter = _stdoutPrinter;
		stderrPrinter = _stderrPrinter;
		environmentVars = _environmentVars;
		workingDirectory = _workingDirectory;
		timeOutMs = _timeOutMs;
	}

	public int runProcess(String[] commands, String dataToPost) throws InterruptedException, IOException {
		InputStream brstdout;
		InputStream brstderr;
		// Execute the process
		if (child != null) {
			throw new RuntimeException("ProcesRunner is already running");
		}

		if (dataToPost != null) {
			if (dataToPost.length() > 0) {
				environmentVars = Tools.appendString(environmentVars, "CONTENT_LENGTH=" + dataToPost.length());
				environmentVars = Tools.appendString(environmentVars, "REQUEST_METHOD=POST");

				// environmentVars = Tools.appendString(environmentVars,
				// "QUERY_STRING=SERVICE=WPS&REQUEST=EXECUTE&VERSION=1.0.0&IDENTIFIER=testcdo_dtdp");

			}
		}

		/*
		 * for(int j=0;j<environmentVars.length;j++){ Debug.println(environmentVars[j]);
		 * }
		 */
		File workingDir = null;
		if (workingDirectory != null) {
			workingDir = new File(workingDirectory);
			if (workingDir.isDirectory() == false || workingDir.exists() == false) {
				workingDir = null;
			}
		}

		child = Runtime.getRuntime().exec(commands, environmentVars, workingDir);

		if (dataToPost != null) {
			if (dataToPost.length() > 0) {
				OutputStreamWriter wr = new OutputStreamWriter(child.getOutputStream());
				wr.write(dataToPost);
				wr.flush();
				wr.close();
				// DebugConsole.println("Putting postdata\n"+dataToPost);
				PrintWriter stdin = new PrintWriter(child.getOutputStream());
				stdin.print(dataToPost);

				stdin.close();
			}
		}
		brstdout = new BufferedInputStream(child.getInputStream());
		brstderr = new BufferedInputStream(child.getErrorStream());
		stdoutThread = new StatusPrinterThread(stdoutPrinter, brstdout);
		stderrThread = new StatusPrinterThread(stderrPrinter, brstderr);
		stdoutPrinter.setPID(Long.toString(child.pid()));
		stderrPrinter.setPID(Long.toString(child.pid()));
		stdoutThread.start();
		stderrThread.start();

		// Wait for the process to complete
		if (timeOutMs <= 0) {
			child.waitFor();
		} else {
			if (!child.waitFor(timeOutMs, TimeUnit.MILLISECONDS)) {
				Debug.errprintln("[ADAGUC-Server] TimeOut");
				child.destroy(); // consider using destroyForcibly instead
			}
		}

		// Wait for the output monitoring threads to complete
		stdoutThread.join();
		stderrThread.join();
		if (child != null) {
			try {
				child.getOutputStream().close();
			} catch (Exception e) {
				Debug.errprintln("Output stream was already closed");
			}
			child.getInputStream().close();
			child.getErrorStream().close();
			child.destroy();
		}

		exitCode = child.exitValue();

		return exitCode;
	}

	public int exitValue() {
		return exitCode;
	}

	public void abort() {
		if (child != null) {
			try {
				child.getOutputStream().close();
				child.getInputStream().close();
				child.getErrorStream().close();
			} catch (IOException e) {
				String msg = "Exception in ProcessRunner while aborting: " + e.getMessage();
				stderrPrinter.print(msg.getBytes(), msg.length());
			}
			child.destroy();
		}
	}

}