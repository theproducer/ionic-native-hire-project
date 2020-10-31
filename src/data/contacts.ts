import { Plugins } from "@capacitor/core";

const { Contacts } = Plugins;

export interface Contact {
  id: string;
  firstName: string;
  lastName: string;
  phoneNumbers: string[];
  emailAddresses: string[];
}

export enum ContactSearchType {
  All = "ALL",
  Name = "NAME",
  Phone = "PHONE",
  Email = "EMAIL",
}

export const getContacts = async (): Promise<Contact[]> => {
  try {
    const result = await Contacts.getAll();
    return result.contacts;
  } catch (e) {
    console.error(`ERR (${getContacts.name}):`, e);
  }

  return [];
};

export const getContactById = async (contactId: string): Promise<Contact|null> => {
  const result = await Contacts.getById({
    contactId: contactId,
  });

  return result.contact;
};

export const queryContacts = async (
  searchTerm: string,
  groupName?: string
): Promise<Contact[]> => {
  try {
    const result = await Contacts.getBySearch({
      searchTerm: searchTerm,
      groupName: groupName,
    });
    return result.contacts;
  } catch (e) {
    console.log(`ERR (${queryContacts.name}):`, e);
  }

  return [];
};
