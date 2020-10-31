import ContactListItem from "../components/ContactListItem";
import React, { useEffect, useState } from "react";
import {
  Contact,
  getContacts,
  queryContacts,
  getContactById,
} from "../data/contacts";
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
import ContactModal from "../components/ContactModal";

const Home: React.FC = () => {
  const [showContact, setShowContact] = useState<boolean>(false);
  const [contact, setContact] = useState<Contact | null>(null);
  const [searchTerm, setSearchTerm] = useState<string | undefined>(undefined);
  const [contacts, setContacts] = useState<Contact[]>([]);

  const fetchContacts = async (searchTerm?: string) => {
    if (searchTerm) {
      const msg = await queryContacts(searchTerm);
      setContacts(msg);
    } else {
      const msgs = await getContacts();
      setContacts(msgs);
    }
  };

  const onContactClick = async (id: string) => {
    console.log(id);
    const contact = await getContactById(id);
    setContact(contact);
    setShowContact(true);
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
        <ContactModal
          showModel={showContact}
          setShowContact={setShowContact}
          contact={contact}
        />
        <IonHeader collapse="condense">
          <IonToolbar>
            <IonTitle size="large">Contacts</IonTitle>
          </IonToolbar>
        </IonHeader>
        <IonSearchbar
          placeholder="Search contacts"
          onIonChange={(e) => {
            setSearchTerm(e.detail.value || "");
          }}
          debounce={750}
        />
        <IonList>
          {contacts.map((contact) => (
            <ContactListItem
              key={contact.id}
              contact={contact}
              onContactClick={onContactClick}
            />
          ))}
        </IonList>
      </IonContent>
    </IonPage>
  );
};

export default Home;
