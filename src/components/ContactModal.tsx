import React from "react";
import {
  IonModal,
  IonHeader,
  IonTitle,
  IonToolbar,
  IonContent,
  IonButton,
  IonList,
  IonItem,
  IonListHeader,
  IonLabel,
} from "@ionic/react";
import { Contact } from "../data/contacts";
import "./ContactModal.css";

interface ContactModalProps {
  contact: Contact | null;
  showModel: boolean;
  setShowContact: (show: boolean) => void;
}

const ContactModal: React.FC<ContactModalProps> = ({
  contact,
  showModel,
  setShowContact,
}) => {
  return (
    <IonModal isOpen={showModel}>
      {contact && (
        <IonContent fullscreen>
          <IonHeader>
            <IonToolbar>
              <IonTitle>
                {contact.firstName} {contact.lastName}
              </IonTitle>
            </IonToolbar>
          </IonHeader>
          <div className="padding contactHeader">
            <h3>
              {contact.firstName} {contact.lastName}
            </h3>
          </div>

          <IonList>
            <IonListHeader lines="full">
              <IonLabel>Email addresses</IonLabel>
            </IonListHeader>
            {contact.emailAddresses.map((email) => {
              return (
                <IonItem>
                  <IonLabel>{email}</IonLabel>
                </IonItem>
              );
            })}
          </IonList>
          <IonList>
            <IonListHeader lines="full">
              <IonLabel>Phone numbers</IonLabel>
            </IonListHeader>
            {contact.phoneNumbers.map((phone) => {
              return (
                <IonItem>
                  <IonLabel>{phone}</IonLabel>
                </IonItem>
              );
            })}
          </IonList>
          <div className="padding">
            <IonButton expand="block" onClick={() => setShowContact(false)}>
              Close Contact
            </IonButton>
          </div>
        </IonContent>
      )}
    </IonModal>
  );
};

export default ContactModal;
