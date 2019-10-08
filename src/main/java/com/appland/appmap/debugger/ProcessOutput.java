// package com.appland.appmap.debugger;

// import java.io.IOException;
// import java.io.InputStream;
// import java.io.PrintStream;

// public class ProcessOutput implements Runnable {
//   private Process process;
//   private Boolean running = false;

//   public ProcessOutput(Process process) {
//     this.process = process;
//   }

//   private void redirectOutput(InputStream inputStream, PrintStream outputStream) {
//     try {
//       if (inputStream.available() == 0) {
//         return;
//       }

//       outputStream.print(inputStream.readAllBytes());
//     } catch (IOException e) {
//       System.err.println(e.getMessage());
//     }
//   }

//   public void stop() {
//     // this could use a mutex, but if the main loop runs again it's okay
//     running = false;
//   }

//   public void run() {
//     running = true;

//     for (;;) {
//       if (!running) {
//         break;
//       }

//       redirectOutput(process.getInputStream(), System.out);
//       redirectOutput(process.getErrorStream(), System.err);

//       try {
//         Thread.sleep(16);
//       } catch (InterruptedException e) {
//         // do nothing
//       }
      
//     }
//   }
// }