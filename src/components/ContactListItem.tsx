import React from "react";
import { IonItem, IonLabel, IonNote } from "@ionic/react";
import { Contact } from "../data/contacts";
import "./ContactListItem.css";

interface ContactListItemProps {
  contact: Contact;
  onContactClick: (id: string) => void;
}

const ContactListItem: React.FC<ContactListItemProps> = ({
  contact,
  onContactClick,
}) => {
  return (
    <IonItem
      onClick={() => {
        onContactClick(contact.id);
      }}
    >
      <IonLabel className="ion-text-wrap">
        <h2>
          {contact.firstName} {contact.lastName}
          <span className="date">
            <IonNote>{contact.phoneNumbers[0]}</IonNote>
          </span>
        </h2>
        <h3>{contact.emailAddresses[0]}</h3>
      </IonLabel>
    </IonItem>
  );
};

export default ContactListItem;
