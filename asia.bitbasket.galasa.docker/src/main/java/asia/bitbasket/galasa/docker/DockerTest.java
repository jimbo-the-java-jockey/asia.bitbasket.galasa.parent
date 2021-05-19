package asia.bitbasket.galasa.docker;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import dev.galasa.Test;
import dev.galasa.core.manager.Logger;
import dev.galasa.docker.*;
import dev.galasa.http.IHttpClient;
import dev.galasa.http.HttpClient;
import dev.galasa.http.HttpClientException;
import dev.galasa.http.HttpClientResponse;

@Test
public class DockerTest {
	
	@Logger
	public Log logger;
	
	// BUG: docs say "tag", not "dockerContainerTag"
	@DockerContainer(image="library/httpd:latest", dockerContainerTag="apache", start=true)
	public IDockerContainer apache;
	
	public IDockerExec exec;
	
	@HttpClient
	public IHttpClient client;
	
	@Test
	public void checkContainerNotNull() {
		
		assertThat(apache).isNotNull();
	}
	
	@Test
	public void startContainerTest() throws DockerManagerException {
		
		assertThat(apache.isRunning()).isTrue();
	}
	
	@Test
	public void checkInsideContainer() throws DockerManagerException {
		
		exec = apache.exec("ps -a | grep httpd");
		exec.waitForExec();
		assertThat(exec.getCurrentOutput()).contains("httpd");
	}
	
	
	@Test
	public void checkOpenPorts() throws DockerManagerException {
		
		Map<String, List<InetSocketAddress>> openPorts = apache.getExposedPorts();
		
		// per Javadoc, getExposedPorts() should never be null
		assertThat(openPorts).isNotNull();
		
		// default HTTP port must be open
		// BUG: Javadoc says tcp/80, not 80/tcp
		logger.info("openPorts: "+openPorts.toString());
		assertThat(openPorts).containsKey("80/tcp");
		
		// should not be listening for SMTP
		assertThat(openPorts).doesNotContainKey("25/tcp");
	}
	
	@Test
	public void checkListenAddress() {
		InetSocketAddress listenAddress = apache.getFirstSocketForExposedPort("80/tcp");
		
		assertThat(listenAddress).isNotNull();
		logger.info("First socket for port 80 is "+listenAddress.getHostString());
	}
	
	@Test
	public void connectViaWeb() throws HttpClientException, URISyntaxException {
		
		InetSocketAddress listenAddress = apache.getFirstSocketForExposedPort("80/tcp");
		String containerURI = String.format("http://%s:%d", listenAddress.getHostString(), listenAddress.getPort());
		
		logger.info("Connecting to "+containerURI);

		client.setURI(new URI(containerURI));
		HttpClientResponse<String> response = client.getText("/");
		assertThat(response).isNotNull();
		assertThat(response.getStatusCode()).isEqualTo(200);

		logger.info("The response body was: "+response.getContent());
		assertThat(response.getContent().contains("It works!"));
	}

	@Test
	public void stopContainerTest() throws DockerManagerException {
		
		apache.stop();
		assertThat(apache.isRunning()).isFalse();
	}
	
}