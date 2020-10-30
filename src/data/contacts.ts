import { Plugins } from '@capacitor/core';

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
  Email = "EMAIL"
};

export const getContacts = async (searchTerm?: string): Promise<Contact[]> => {
  try {
    if (searchTerm !== "") {
      const result = await Contacts.getBySearch({searchTerm: searchTerm, searchType: ContactSearchType.All});
      return result.contacts;
    }
    
    const result = await Contacts.getAll();
    return result.contacts;
  } catch (e) {
    console.error(`ERR (${getContacts.name}):`, e);
  }

  return [];
};
