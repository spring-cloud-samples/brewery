package io.spring.cloud.samples.brewery.zookeeper;

import org.apache.zookeeper.server.ZooKeeperServerMain;

public class Application {

    public static void main(String[] args) {
        ZooKeeperServerMain.main(new String[] { "2181", "/tmp/zookeeper" });
    }
}
