
export enum AnnouncementCategory {
    PUBLIC_SAFETY = 'PUBLIC_SAFETY',
    UTILITIES = 'UTILITIES',
    ADMINISTRATIVE = 'ADMINISTRATIVE',
    HEALTH_ADVISORY = 'HEALTH_ADVISORY',
    GENERAL = 'GENERAL'
}

export enum EventCategory {
    TOWN_HALL = 'TOWN_HALL',
    SPORTS_RECREATION = 'SPORTS_RECREATION',
    CULTURAL_RELIGIOUS = 'CULTURAL_RELIGIOUS',
    FESTIVAL = 'FESTIVAL',
    VOLUNTEER_WORK = 'VOLUNTEER_WORK'
}

export enum ProgramCategory {
    HEALTH_AND_WELLNESS = 'HEALTH_AND_WELLNESS',
    EDUCATION_YOUTH = 'EDUCATION_YOUTH',
    LIVELIHOOD_SKILLS = 'LIVELIHOOD_SKILLS',
    SOCIAL_WELFARE = 'SOCIAL_WELFARE',
    ENVIRONMENTAL = 'ENVIRONMENTAL'
}

export const getCategoryLabel = (category: string): string => {
    const labels: { [key: string]: string } = {
        // Announcements
        'PUBLIC_SAFETY': 'Public Safety & Health',
        'UTILITIES': 'Infrastructure & Utilities',
        'ADMINISTRATIVE': 'Administrative',
        'HEALTH_ADVISORY': 'Health Advisory',
        'GENERAL': 'General News',
        // Events
        'TOWN_HALL': 'Town Hall & Forum',
        'SPORTS_RECREATION': 'Sports & Recreation',
        'CULTURAL_RELIGIOUS': 'Cultural & Religious',
        'FESTIVAL': 'Festival',
        'VOLUNTEER_WORK': 'Volunteer Work',
        // Programs
        'HEALTH_AND_WELLNESS': 'Health & Wellness',
        'EDUCATION_YOUTH': 'Education & Youth',
        'LIVELIHOOD_SKILLS': 'Livelihood & Skills',
        'SOCIAL_WELFARE': 'Social Welfare',
        'ENVIRONMENTAL': 'Environmental'
    };
    return labels[category] || category;
};
