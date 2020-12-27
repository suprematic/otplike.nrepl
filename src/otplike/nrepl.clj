(ns otplike.nrepl
  (:require
    [nrepl.server :as nrepl]
    [nrepl.transport :as nrepl.transport]
    [otplike.process :as process]
    [otplike.logger :as log]
    [otplike.supervisor :as supervisor]
    [otplike.gen-server :as gs]))

(defn init [port' bind]
  (process/flag :trap-exit true)

  [:ok
   {:nrepl
    (let
      [transport #'nrepl.transport/bencode
       {:keys [server-socket port] :as nrepl}
       (nrepl/start-server :port port' :bind bind :transport-fn transport)]
      (let
        [host (.getHostName (.getInetAddress server-socket))
         scheme (nrepl.transport/uri-scheme transport)]
        (when (= port' 0)
          (spit "nrepl-port" port))

        (log/info "nREPL server started: uri=~{scheme}://~{host}:~{port}" :scheme scheme :host host :port port))
      nrepl)}])

(defn handle-info [_ state]
  [:noreply state])

(defn terminate [_reason {:keys [nrepl]}]
  (nrepl/stop-server nrepl)
  (log/info "nREPL server terminated"))

(defn start-link [port bind]
  (gs/start-link-ns ::nrepl [port bind] {}))

(defn- sup-fn [port bind]
  [:ok
   [{:strategy :one-for-all}
    [{:id :nrepl :start [start-link [port bind]]}]]])

(defn start [{:keys [port bind]}]
  (supervisor/start-link sup-fn [port bind]))