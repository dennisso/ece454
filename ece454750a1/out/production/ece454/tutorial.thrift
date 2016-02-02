namespace java ece454750s15a1

struct PerfCounters {
  // number of seconds since service startup
  1:i32 numSecondsUp,
  
  // total number of requests received by service handler
  2:i32 numRequestsReceived,
  
  // total number of request completed by service handler
  3:i32 numrequestsCompleted
}

enum MessageType {
  ARRIVAL = 1,
  DEAD = 2
}

enum NodeType {
  BE = 1,
  FE = 2
}

struct Event {
  // value from MessageType enum
  1:MessageType status,

  // Map ip (string) to port (i32)
  2:map<string, string> msg,

  // Timestamp of Event
  3:i32 timestamp
}

exception ServiceUnavailableException {
  1: string msg
}

service A1Password {

  string hashPassword(1:string password, 2:i16 logRounds) throws (1:ServiceUnavailableException e),

  bool checkPassword(1:string password, 2:string hash) throws (1:ServiceUnavailableException e),

}

service A1Management {
  list<string> getGroupMembers(),

  PerfCounters getPerfCounters(),

  oneway void gossip(1:Event event)
}