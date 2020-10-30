import ContactListItem from "../components/ContactListItem";
import React, { useEffect, useState } from "react";
import { Contact, ContactSearchType, getContacts } from "../data/contacts";
import {
  IonContent,
  IonHeader,
  IonList,
  IonPage,
  IonTitle,
  IonToolbar,
  IonSearchbar,
  
  useIonViewWillEnter,
} from "@ionic/react";

import "./Home.css";

const Home: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState<string | undefined>(undefined);
  const [contacts, setContacts] = useState<Contact[]>([]);

  const fetchContacts = async (searchTerm?: string) => {
    const msgs = await getContacts(searchTerm);
    setContacts(msgs);
  };

  useIonViewWillEnter(async () => {
    await fetchContacts();
  });

  useEffect(() => {
    if (searchTerm !== undefined) {
      fetchContacts(searchTerm);
    }    
  }, [searchTerm]);

  return (
    <IonPage id="home-page">
      <IonHeader>
        <IonToolbar>
          <IonTitle>Contacts</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent fullscreen>
        <IonHeader collapse="condense">
          <IonToolbar>
            <IonTitle size="large">Contacts</IonTitle>
          </IonToolbar>
          <IonToolbar>
            <IonSearchbar
              placeholder="Search contacts"
              onIonChange={(e) => {
                setSearchTerm(e.detail.value || "");                
              }}       
              debounce={750}       
            />
          </IonToolbar>
        </IonHeader>

        <IonList>
          {contacts.map((contact) => (
            <ContactListItem key={contact.id} contact={contact} />
          ))}
        </IonList>
      </IonContent>
    </IonPage>
  );
};

export default Home;
