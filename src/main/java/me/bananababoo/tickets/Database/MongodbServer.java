package me.bananababoo.tickets.Database;

import com.google.gson.Gson;
import com.mongodb.AuthenticationMechanism;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import me.bananababoo.tickets.TicketStuff.Ticket;
import me.bananababoo.tickets.Tickets;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;

import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.*;


public class MongodbServer {

    public static MongoDatabase database;
    public static MongoCollection<Document> col;
    public static int currentid;

    public static void connect(){

        MongoCredential credential = MongoCredential.createCredential("Banana","ba6lhd4vitblpfq", "Baboo".toCharArray()).withMechanism(AuthenticationMechanism.SCRAM_SHA_1);
        ConnectionString connectionString = new ConnectionString("mongodb://n2-c2-mongodb-clevercloud-customers.services.clever-cloud.com:27017");

        MongoClientSettings settings = MongoClientSettings.builder()
                .credential(credential)
                .applyConnectionString(connectionString)
                .build();
        MongoClient mongoClient = MongoClients.create(settings);

        database = mongoClient.getDatabase("ba6lhd4vitblpfq"); //using clever cloud so can't change
        col = database.getCollection("TicketStorage");

        currentid = getLastId();

        Bukkit.getLogger().info("Connected to MongoDB");
    }

    public static void saveTicketAsync(Ticket ticket){
        Bukkit.getScheduler().runTaskAsynchronously(Tickets.getPlugin(), () -> {
            Document doc = Document.parse(new Gson().toJson(ticket)); // turn the ticket into json and parse it
            if (col.findOneAndReplace(or(eq(ticket._ID()), all("id", ticket.ID())), doc) == null) {    // if a ticket already exists with the same id, overwrite ticket with new data
                col.insertOne(doc);   //else save it in a new doc
                Bukkit.getLogger().info(ticket + "\n Got Updated to DB ");
            }
        });
    }

    public static void removeTicketAsync(Ticket ticket){
        Bukkit.getScheduler().runTaskAsynchronously(Tickets.getPlugin(), () -> {
            Bson filter = eq(ticket._ID());
            Bson filter2 = Filters.all("id", ticket.ID());
            Bukkit.getLogger().info((col.find().toString()));
            Bukkit.getLogger().info("2");
            if (col.findOneAndDelete(or(filter,filter2)) != null) {    // if a ticket exists with the id remove it
                Bukkit.getLogger().info(ticket.name() + "\n Ticket Got Removed from DB ");
            }else {
                Bukkit.getLogger().info(ticket.name() + "\n Ticket Didn't get removed as it dosn't exist");
            }
        });
    }

    public static void findTicketsAsync(final QueryCallback callback){
        Bukkit.getScheduler().runTaskAsynchronously(Tickets.getPlugin(), () -> {
            //async find all Tickets in collection
            final FindIterable<Document> docs = col.find();
            Bukkit.getScheduler().runTask(Tickets.getPlugin(), () -> {
                // call the callback with the Tickets
                callback.onQueryFinished(docs);
            });
        });
    }

    public static Integer getLastId(){
        Integer result = 0;
        try {
            result = col.find().sort(Sorts.descending("id")).first().getInteger("id");
        } catch (Exception e) {
            Bukkit.getLogger().info("1st Ticket stored");
            Bukkit.getLogger().warning(e.getMessage());
        }
        return result;
    }
    public static Integer getCurrentId(){
        currentid += 1;
        return currentid;
    }
    public static long getNumOfDocs(){
        return col.countDocuments();
    }

}

