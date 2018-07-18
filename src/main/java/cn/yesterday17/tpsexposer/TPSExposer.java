package cn.yesterday17.tpsexposer;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.world.DimensionType;
import org.apache.logging.log4j.Logger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;

import static com.google.common.math.DoubleMath.mean;

@Mod(modid = "tpsexposer", name = "TPSExposer", version = "1.0.0", serverSideOnly = true)
@SideOnly(Side.SERVER)
public class TPSExposer {
    private static Logger logger;
    private HttpServer server;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        try {
            server =  HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/", new TPSHandler(event.getServer()));
            server.start();
        } catch(Exception e){
            logger.error(e.getMessage());
        }
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event){
        this.server.stop(0);
    }

    static class TPSHandler implements HttpHandler{
        private MinecraftServer server;

        TPSHandler(MinecraftServer server) {
            this.server = server;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = new GameTPS(server).getTpsMessage();
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class GameTPS {
        private static final DecimalFormat TIME_FORMATTER = new DecimalFormat("########0.000");
        private String tpsMessage = "[\n  ";

        GameTPS(MinecraftServer server){
            for (Integer dimId : DimensionManager.getIDs())
            {
                DimensionType providerType = DimensionManager.getProviderType(dimId);
                double worldTickTime = mean(server.worldTickTimes.get(dimId)) * 1.0E-6D;
                double worldTPS = Math.min(1000.0/worldTickTime, 20);
                tpsMessage += "{\n    \"name\": \"" + providerType.getName() + "\",\n    \"id\": \"" + dimId + "\",\n    \"tps\": \"" + TIME_FORMATTER.format(worldTPS) + "\",\n    \"tickTime\": \"" + TIME_FORMATTER.format((worldTickTime)) + "\"\n  },\n";
            }
            double meanTickTime = mean(server.tickTimeArray) * 1.0E-6D;
            double meanTPS = Math.min(1000.0/meanTickTime, 20);
            tpsMessage += "  {\n    \"name\": \"Overall\",\n    \"id\": \"undefined\",\n    \"tps\": \"" + TIME_FORMATTER.format(meanTPS) + "\",\n    \"tickTime\": \"" + TIME_FORMATTER.format((meanTickTime)) + "\"\n  }\n";
            tpsMessage += "]";
        }

        public String getTpsMessage(){
            return this.tpsMessage;
        }
    }
}
