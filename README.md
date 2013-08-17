# bulwark

Throttling / IP banning library for Ring-compatible Clojure apps based on Kickstarter's Rack::attack found at https://github.com/kickstarter/rack-attack

## Usage

Add to :dependencies in your Leiningen project.clj:

    [bulwark "0.0.2"]
    [com.taoensso/carmine "2.2.0"] ;; if you're using the default Redis persistence layer for throttling.

Add the protect middleware to your app var. Example from neubite:

    (def app (-> (apply routes all-routes)
                 (user-middleware)
                 (protect-middleware)
                 (site {:session {:cookie-name "session"
                                  :store (cookie-store
                                          {:key (config :secret)})}})
                 (middleware/wrap-request-map)
                 (wrap-multipart-params)))

To blacklist localhost:

    ;; [name fn]
    (blacklist "I hate localhost" (fn [req] (not= (:remote-addr req) "127.0.0.1")))

To whitelist localhost:

    ;; [name fn]
    (whitelist "I love localhost" (fn [req] (not= (:remote-addr req) "127.0.0.1")))

To throttle by a particular key, whatever key you return in the passed fn counts the hits and decides the totals for the throttling intervals.

    ;; [name limit period id-fn]
    (throttle "Slow your roll son" 1 5 (fn [req] (:remote-addr req)))

Period is in seconds. 1 request allowed every 5 seconds in the above example. You can key by arbitrary application data. I recommend using user-middleware like I do to inject user sessions/id into the request before the protect middleware.

## For non-muggles

You can pass a config as a trailing argument like

    (protect-middleware app config)

And the returned fn will use the private closure state instead of the global atom for testing purposes (or whatever).

I added this so I could test bulwark easily and also so the Clojure IRC channel doesn't flay my hide.

## License

Copyright © 2013 Chris Allen

Distributed under the Eclipse Public License, the same as Clojure.
