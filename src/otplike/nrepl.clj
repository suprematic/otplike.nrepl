(ns otplike.nrepl
  (:require
   [clojure.java.io :as io]

   [nrepl.server]
   [nrepl.cmdline]
   [nrepl.transport]
   [nrepl.socket]

   [otplike.process :as process]
   [otplike.logger :as log]
   [otplike.supervisor :as supervisor]
   [otplike.gen-server :as gs]))

(defn init [opts]
  (process/flag :trap-exit true)

  [:ok
   {:nrepl
    (let
      [{:keys [server-socket port transport] :as nrepl}
       (let [{:keys [port bind socket handler transport greeting]} (nrepl.cmdline/server-opts opts)]
         (nrepl.server/start-server
           :port port
           :bind bind
           :socket socket
           :handler handler
           :transport-fn transport
           :greeting-fn greeting))]

      (let [port-file (io/file ".nrepl-port")]
        (.deleteOnExit port-file)
        (spit port-file port))

      (let [uri (nrepl.socket/as-nrepl-uri server-socket (nrepl.transport/uri-scheme transport))]
        (log/info
          {:what :start
           :log :trace
           :details
           {:port port
            :host (.getHost uri)
            :uri uri}}))
      nrepl)}])

(defn handle-info [_ state]
  [:noreply state])

(defn terminate [_reason {:keys [nrepl]}]
  (nrepl.server/stop-server nrepl)
  (log/info
    {:what :stop
     :log :event}))

(defn start-link [env]
  (gs/start-link-ns ::nrepl [env] {}))

(defn- sup-fn [env]
  [:ok
   [{:strategy :one-for-all}
    [{:id :nrepl :start [start-link [env]]}]]])

(defn start [env]
  (supervisor/start-link sup-fn [env]))