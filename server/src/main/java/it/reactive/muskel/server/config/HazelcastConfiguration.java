package it.reactive.muskel.server.config;

import static com.hazelcast.config.ExecutorConfig.DEFAULT_POOL_SIZE;
import static com.hazelcast.config.MulticastConfig.DEFAULT_MULTICAST_GROUP;
import static com.hazelcast.config.MulticastConfig.DEFAULT_MULTICAST_PORT;
import it.reactive.muskel.context.MuskelManagedContext;
import it.reactive.muskel.context.hazelcast.HazelcastMuskelContext;
import it.reactive.muskel.context.impl.AppendableManagedContext;
import it.reactive.muskel.exceptions.MuskelException;
import it.reactive.muskel.server.context.MuskelInjectAnnotationManagedContext;
import it.reactive.muskel.server.hazelcast.classloader.repository.HazelcastClassLoaderRepository;
import it.reactive.muskel.server.hazelcast.context.CompositeManagedContext;
import it.reactive.muskel.server.hazelcast.context.HazelcastInstanceMuskelContext;
import it.reactive.muskel.server.hazelcast.context.HazelcastManagedContextWrapper;
import it.reactive.muskel.server.hazelcast.context.HazelcastSpringManagedContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.esotericsoftware.minlog.Log;
import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ManagedContext;
import com.hazelcast.instance.HazelcastInstanceFactory;
import com.hazelcast.spring.context.SpringManagedContext;

@Configuration
public class HazelcastConfiguration {

    @Value("${name:muskel}")
    private String groupName;

    @Value("${password:password}")
    private String groupPassword;

    @Value("${instanceName:muskel-server}")
    private String instanceName;

    @Value("${portNumber:5701}")
    private int portNumber;

    @Value("${portAutoIncrement:true}")
    private boolean portAutoIncrement;

    @Value("${groups:}")
    public String memberSelectorValue;

    @Value("${discoveryMulticastGroup:" + DEFAULT_MULTICAST_GROUP + "}")
    private String discoveryMulticastGroup;
    @Value("${discoveryMulticastPort:" + DEFAULT_MULTICAST_PORT + "}")
    private int discoveryMulticastPort;
    @Value("${discoveryTcpMembers:}")
    private String discoveryTcpMembers;

    @Value("${clientPoolSize:" + DEFAULT_POOL_SIZE + "}")
    private int clientPoolSize;

    @Value("${sslEnabled:false}")
    private boolean sslEnabled;

    @Value("${sslKeyStore:}")
    private String sslKeyStore;

    @Value("${sslKeyStorePassword:}")
    private String sslKeyStorePassword;

    @Value("${sslKeyManagerAlgorithm:}")
    private String sslKeyManagerAlgorithm;

    @Value("${sslTrustManagerAlgorithm:}")
    private String sslTrustManagerAlgorithm;

    @Value("${sslProtocol:}")
    private String sslProtocol;

    @Value("${networkInterfaces:}")
    private String networkInterfaces;

    @Bean
    public ManagedContext createSpringManagedContext() {
	return new CompositeManagedContext(new SpringManagedContext(),
		new HazelcastSpringManagedContext());
    }

    @Bean
    public MuskelManagedContext createMuskelManagedContext(
	    ApplicationContext applicationContext) {
	return new AppendableManagedContext(new HazelcastInstanceMuskelContext(
		createHazelcastInstance()), new HazelcastManagedContextWrapper(
		createSpringManagedContext()),
		new MuskelInjectAnnotationManagedContext(applicationContext));
    }

    @Bean
    public HazelcastClassLoaderRepository classLoaderRepository() {
	return new HazelcastClassLoaderRepository(createHazelcastInstance());
    }

    @Bean
    public HazelcastInstance createHazelcastInstance() {
	Config config = new Config();
	config.setProperty("hazelcast.logging.type", "slf4j");
	config.setGroupConfig(new GroupConfig(groupName, groupPassword));
	config.setInstanceName(instanceName);
	config.getNetworkConfig().setPort(portNumber);
	if (memberSelectorValue != null
		&& memberSelectorValue.trim().length() > 0) {
	    config.getMemberAttributeConfig().setStringAttribute(
		    HazelcastMuskelContext.MEMBERSELECTOR_ATTRIBUTE_KEY,
		    memberSelectorValue.trim());
	}

	if (sslEnabled) {

	    config.getNetworkConfig().setSSLConfig(
		    new SSLConfig()
			    .setEnabled(true)
			    .setEnabled(sslEnabled)
			    .setProperty("keyStore", sslKeyStore)
			    .setProperty("keyStorePassword",
				    sslKeyStorePassword)
			    .setProperty("keyManagerAlgorithm",
				    sslKeyManagerAlgorithm)
			    .setProperty("trustManagerAlgorithm",
				    sslTrustManagerAlgorithm)
			    .setProperty("protocol", sslProtocol));
	}

	config.getNetworkConfig().setPortAutoIncrement(portAutoIncrement);
	config.setManagedContext(createSpringManagedContext());

	config.getExecutorConfig("default").setPoolSize(clientPoolSize);
	/*
	 * RingbufferConfig rbConfig = new RingbufferConfig("pippo")
	 * .setCapacity(20) .setBackupCount(1) .setAsyncBackupCount(0)
	 * .setTimeToLiveSeconds(0) .setInMemoryFormat(InMemoryFormat.BINARY);
	 * config.addRingBufferConfig(rbConfig);
	 */
	// if (env.acceptsProfiles(Constants.SPRING_PROFILE_DEVELOPMENT)) {
	// System.setProperty("hazelcast.local.localAddress", "127.0.0.1");

	boolean discoveryTcpEnabled = StringUtils.hasText(discoveryTcpMembers);
	boolean discoveryMulticastEnabled = StringUtils
		.hasText(discoveryMulticastGroup)
		&& (!discoveryTcpEnabled || !DEFAULT_MULTICAST_GROUP
			.equals(discoveryMulticastGroup));

	if (discoveryTcpEnabled && discoveryMulticastEnabled) {
	    Log.error("You cannot select both tcp and multicast");
	    throw new MuskelException(
		    "You cannot select both tcp and multicast");
	}

	NetworkConfig networkConfig = config.getNetworkConfig();

	if (StringUtils.hasText(networkInterfaces)) {
	    InterfacesConfig interfacesConfig = networkConfig.getInterfaces();

	    for (String current : networkInterfaces.split(",")) {
		if (StringUtils.hasText(current))
		    interfacesConfig.setEnabled(true).addInterface(current);
	    }
	}
	networkConfig.getJoin().getAwsConfig().setEnabled(false);

	networkConfig.getJoin().getTcpIpConfig()
		.setEnabled(discoveryTcpEnabled);

	networkConfig.getJoin().getMulticastConfig()
		.setEnabled(discoveryMulticastEnabled);

	if (discoveryMulticastEnabled) {
	    networkConfig.getJoin().getMulticastConfig()
		    .setMulticastGroup(discoveryMulticastGroup)
		    .setMulticastPort(discoveryMulticastPort);
	}
	if (discoveryTcpEnabled) {
	    networkConfig.getJoin().getTcpIpConfig()
		    .addMember(discoveryTcpMembers);
	}

	// }

	config.getMapConfigs().put("default", initializeDefaultMapConfig());

	return HazelcastInstanceFactory.newHazelcastInstance(config);
    }

    private MapConfig initializeDefaultMapConfig() {
	MapConfig mapConfig = new MapConfig();

	/*
	 * Number of backups. If 1 is set as the backup-count for example, then
	 * all entries of the map will be copied to another JVM for fail-safety.
	 * Valid numbers are 0 (no backup), 1, 2, 3.
	 */
	mapConfig.setBackupCount(0);

	/*
	 * Valid values are: NONE (no eviction), LRU (Least Recently Used), LFU
	 * (Least Frequently Used). NONE is the default.
	 */
	mapConfig.setEvictionPolicy(EvictionPolicy.LRU);

	/*
	 * Maximum size of the map. When max size is reached, map is evicted
	 * based on the policy defined. Any integer between 0 and
	 * Integer.MAX_VALUE. 0 means Integer.MAX_VALUE. Default is 0.
	 */
	mapConfig.setMaxSizeConfig(new MaxSizeConfig(0,
		MaxSizeConfig.MaxSizePolicy.USED_HEAP_SIZE));

	/*
	 * When max. size is reached, specified percentage of the map will be
	 * evicted. Any integer between 0 and 100. If 25 is set for example, 25%
	 * of the entries will get evicted.
	 */
	mapConfig.setEvictionPercentage(25);

	return mapConfig;
    }

}
