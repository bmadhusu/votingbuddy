(ns votingbuddy.routes.services
  (:require
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
;;    [ring-swagger :as swagger]
;;    [swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [votingbuddy.endorsements :as endorse]
   [votingbuddy.middleware :as middleware]
   [ring.util.http-response :as response]
   [votingbuddy.middleware.formats :as formats]
   [votingbuddy.auth :as auth]
   [spec-tools.data-spec :as ds]
   [votingbuddy.auth.ring :refer [wrap-authorized get-roles-from-match]]
   [clojure.tools.logging :as log]))

(defn service-routes []
  ["/api"
   {:middleware [parameters/parameters-middleware
                 muuntaja/format-negotiate-middleware
                 muuntaja/format-response-middleware
                 exception/exception-middleware
                 muuntaja/format-request-middleware
                 coercion/coerce-response-middleware
                 coercion/coerce-request-middleware
                 multipart/multipart-middleware
                 ;;our auth middleware
                 ;
                 (fn [handler]
                   (wrap-authorized
                    handler
                    (fn handle-unauthorized [req]
                      (let [route-roles (get-roles-from-match req)]
                        (log/debug "Roles for route: "
                                   (:uri req)
                                   route-roles)
                        (log/debug "user is unauthorized!"
                                   (-> req
                                       :session
                                       :identity
                                       :roles))
                        (response/forbidden
                         {:message
                          (str "User must have one of the following roles: "
                               route-roles)})))))]
    :muuntaja formats/instance
    :coercion spec-coercion/coercion
    :swagger {:id ::api}}
   ["/session"
    {::auth/roles (auth/roles :session/get)
     :get
     {:responses
      {200
       {:body
        {:session
         {:identity (ds/maybe {:login string?
                               :created_at inst?})}}}}
      :handler
      (fn [{{:keys [identity]} :session}]
        (response/ok {:session
                      {:identity
                       (not-empty
                        (select-keys
                         identity [:login :created_at]))}}))}}]
   ["/login"
    {::auth/roles (auth/roles :auth/login)
     :post {:parameters
            {:body
             {:login string?
              :password string?}}
            :responses
            {200
             {:body
              {:identity
               {:login string?
                ;; :first_name nil?
                ;; :last_name nil?
                ;; :admin nil?
                ;; :last_login nil?
                ;; :is_active nil?
                :created_at inst?}}}
             401
             {:body
              {:message string?}}}
            :handler
            (fn [{{{:keys [login password]} :body} :parameters session :session}]
              (if-some [user (auth/authenticate-user login password)]
                ;; (do
                  ;; (println "--BEGIN--")
                  ;; (println session)
                  ;; (println "--END--")
                  ;; (println "Should login user!")

                (-> (response/ok {:identity user})
                    (assoc :session (assoc session :identity user)))
                (response/unauthorized {:message "Incorrect login or password."})))}}]
   ["/register"
    {::auth/roles (auth/roles :account/register)
     :post {:parameters
            {:body
             {:login string?
              :password string?
              :confirm string?}}
            :responses
            {200
             {:body {:message string?}}
             400
             {:body {:message string?}}
             409
             {:body {:message string?}}}
            :handler
            (fn [{{{:keys [login password confirm]} :body} :parameters}]
              (if-not (= password confirm)
                (response/bad-request {:message "Password & Confirm do not match."})
                (try (auth/create-user! login password)
                     (response/ok {:message "User registration successful. Please login."})
                     (catch clojure.lang.ExceptionInfo e (if (= (:votingbuddy/error-id (ex-data e))
                                                                ::auth/duplicate-user)
                                                           (response/conflict {:message "Registration failed! User with login already exists!"})
                                                           (throw e))))))}}]
   ["/logout"
    {::auth/roles (auth/roles :auth/logout)
     :post {:handler (fn [_] (-> (response/ok) (assoc :session nil)))}}]

   ["" {:no-doc true
        ::auth/roles (auth/roles :swagger/swagger)}
    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]
    ["/swagger-ui*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"})}]]
   ["/endorsements"
    {::auth/roles (auth/roles :endorsements/list)}
    [""
     {:get
      {:responses
       {200
        {:body
         {:endorsements
          [{:subject string?
            :candidatename string?
            :statement string?
            :orgname string?
            :timestamp inst?}]}}}
       :handler
       (fn [_]
         (response/ok (endorse/endorsement-list)))}}]
    ["/byauthor/:author"
     {:get
      {:parameters {:path {:author string?}}
       :responses
       {200
        {:body
         {:endorsements
          [{:subject string?
            :candidatename string?
            :statement string?
            :orgname string?
            :timestamp inst?}]}}}
       :handler
       (fn [{{{:keys [author]} :path} :parameters}]
         (response/ok (endorse/endorsements-by-author author)))}}]
    ["/byorg/:orgID"
     {:get
      {:parameters {:path {:orgID int?}}
       :responses
       {200
        {:body
         {:endorsements
          [{:subject string?
            :candidatename string?
            :statement string?
            :orgname string?
            :timestamp inst?}]}}}
       :handler
       (fn [{{{:keys [orgID]} :path} :parameters}]
         (response/ok (endorse/endorsements-by-organization orgID)))}}]
    ["/bycandidate/:candID"
     {:get
      {:parameters {:path {:candID int?}}
       :responses
       {200
        {:body
         {:endorsements
          [{:subject string?
            :candidatename string?
            :statement string?
            :orgname string?
            :timestamp inst?}]}}}
       :handler
       (fn [{{{:keys [candID]} :path} :parameters}]
         (response/ok (endorse/endorsements-by-candidate candID)))}}]
    ["/forcandidate/:candID"
     {:get
      {:parameters {:path {:candID int?}}
       :responses
       {200
        {:body
         {:endorsements
          [{:subject string?
            :candidatename string?
            :statement string?
            :orgname string?
            :timestamp inst?}]}}}
       :handler
       (fn [{{{:keys [candID]} :path} :parameters}]
         (response/ok (endorse/endorsements-for-candidate candID)))}}]
    ["/byaddress/:address"
     {:get
      {:parameters {:path {:address string?}}
       :responses
       {200
        {:body
         {:endorsements
          [{:subject string?
            :candidatename string?
            :statement string?
            :orgname string?
            :timestamp inst?}]}}}
       :handler
       (fn [{{{:keys [address]} :path} :parameters}]
         (response/ok (endorse/endorsements-by-address address)))}}]]
   ["/endorsement"
    {::auth/roles (auth/roles :endorsement/create!)
     :post
     {:parameters
      {:body
       {:subject string?
        :statement string?}}
      :responses
      {200
       {:body map?}
       400
       {:body map?}
       500
       {:errors map?}}
      :handler
      (fn [{{params :body} :parameters :as whole}]
        (try
        ;;  (endorse/save-endorsement! params)
         ;; temporarily stuffing a name/candidate id here
          ;; (println "whole is: " whole)
          ;; (println "params are: " params)
          ;; (endorse/save-endorsement! params)
          (pr-str "in Hnalder code!")
          (response/ok {:status :ok})
          (catch Exception e
            (let [{id        :votingbuddy/error-id
                   errors    :errors}  (ex-data e)]
              (case id
                :validation
                (response/bad-request {:errors errors})
                ;;else
                (response/internal-server-error
                 {:errors
                  {:server-error ["Failed to save endorsement!!!"]}}))))))}}]])
