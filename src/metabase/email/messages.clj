(ns metabase.email.messages
  "Convenience functions for sending templated email messages.  Each function here should represent a single email.
   NOTE: we want to keep this about email formatting, so don't put heavy logic here RE: building data for emails."
  (:require [hiccup.core :refer [html]]
            [metabase.email :as email]
            [metabase.util :as u]))


;;; ### Public Interface

(defn send-new-user-email
  "Format and Send an welcome email for newly created users."
  [first_name email password-reset-url]
  {:pre [(string? first_name)
         (string? email)
         (u/is-email? email)
         (string? password-reset-url)]}
  (let [message-body (html [:html
                            [:body
                             [:p (format "Welcome to Metabase %s!" first_name)]
                             [:p "Your account is setup and ready to go, you just need to set a password so you can login.  Follow the link below to reset your account password."]
                             [:p [:a {:href password-reset-url} password-reset-url]]]])]
    (email/send-message
      "Your new Metabase account is all set up"
      {email email}
      :html message-body)
    ;; return the message body we sent
    message-body))

(defn send-password-reset-email
  "Format and Send an email informing the user how to reset their password."
  [email hostname password-reset-url]
  {:pre [(string? email)
         (u/is-email? email)
         (string? hostname)
         (string? password-reset-url)]}
  (let [message-body (html [:html
                            [:body
                             [:p (str (format "You're receiving this e-mail because you or someone else has requested a password for your user account at %s. " hostname)
                                      "It can be safely ignored if you did not request a password reset. Click the link below to reset your password.")]
                             [:p [:a {:href password-reset-url} password-reset-url]]]])]
    (email/send-message
      "[Metabase] Password Reset Request"
      {email email}
      :html message-body)
    ;; return the message body we sent
    message-body))

(defn send-email-report
  "Format and Send an `EmailReport` email."
  [subject recipients query-result]
  {:pre [(string? subject)
         (vector? recipients)
         (map? query-result)]}
  (let [html-header-row (fn [cols]
                          (into [:tr {:style "background-color: #f4f4f4;"}]
                            (map (fn [col]
                                   [:td {:style "text-align: left; padding: 0.5em; border: 1px solid #ddd; font-size: 12px;"} col]) cols)))
        html-data-row (fn [row]
                        (into [:tr]
                          (map (fn [cell]
                                 ;; TODO - format cell
                                 ;; {{ cell|default_if_none:"N/A"|floatformat:"-2"|default:cell  }}
                                 [:td {:style "border: 1px solid #ddd; padding: 0.5em;"} cell]) row)))
        message-body (html [:html
                            [:head]
                            [:body {:style "font-family: Helvetica Neue, Helvetica, sans-serif; width: 100%; margin: 0 auto; max-width: 800px; font-size: 12px;"}
                             [:div {:class "wrapper" :style "padding: 10px; background-color: #ffffff;"}
                              (into [:table {:style "border: 1px solid #cccccc; width: 100%; border-collapse: collapse;"}]
                                (vector
                                  ;; table header row
                                  (html-header-row (get-in query-result [:data :columns]))
                                  ;; actual data rows
                                  (map (fn [row] (html-data-row row)) (get-in query-result [:data :rows]))))]]])]
    (email/send-message
      subject
      (into {} (map (fn [email] {email email}) recipients))
      :html message-body)
    ;; return the message body we sent
    message-body))
