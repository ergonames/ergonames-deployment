package execute
import configs.Conf
import node.BaseClient
import utils.network

class Client(
    nodeUrl: String = Conf.read("config.json").nodeUrl,
    apiUrl: String = Conf.read("config.json").apiUrl
) extends BaseClient(
      nodeInfo = execute.DefaultNodeInfo(
        nodeUrl,
        apiUrl,
        new network(nodeUrl).getNetworkType
      )
    ) {}
