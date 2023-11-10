﻿using Application.DaoInterface;
using Domain.DTOs;
using Domain.Models;

namespace obsolete_FileData.DAOs;

public class AnnouncementFileDao : IAnnouncementDao
{
    private readonly FileContext context;

    public AnnouncementFileDao(FileContext context)
    {
        this.context = context;
    }

    public Task<Announcement> CreateAsync(AnnouncementCreationDto dto)
    {
        Console.WriteLine($"Print DateOfCreation Before {dto.CreationDateTime.ToShortDateString()}");
        var announcement = new Announcement
        {
            PetOwner = new PetOwner(context.Users.First(u => u.Email.Equals(dto.OwnerEmail))),
            ServiceDescription = dto.ServiceDescription,
            PostalCode = dto.PostalCode,
            StartDate = dto.StartDate,
            Pet = dto.Pet,
            CreationDateTime = dto.CreationDateTime,
            EndDate = dto.EndDate
        };
        return CreateAsync(announcement);
    }

    public Task<IEnumerable<Announcement>> GetAsync(SearchAnnouncementDto searchParameters)
    {
        IEnumerable<Announcement> announcements = context.Announcements.AsEnumerable();
        if (!string.IsNullOrEmpty(searchParameters.StartTime))
        {
            announcements = announcements.Where(t => t.StartDate.Equals(DateTime.Parse(searchParameters.StartTime)));
        }
        
        if (!string.IsNullOrEmpty(searchParameters.StartTime))
        {
            announcements = announcements.Where(t => t.StartDate.Equals(DateTime.Parse(searchParameters.StartTime)));
        }

        if (!string.IsNullOrEmpty(searchParameters.DescriptionContains))
        {
            announcements = announcements.Where(t => t.ServiceDescription.Contains(searchParameters.DescriptionContains, StringComparison.OrdinalIgnoreCase));
        }

        if (!string.IsNullOrEmpty(searchParameters.PostalCode))
        {
            announcements = announcements.Where(t => t.PostalCode.Equals(searchParameters.PostalCode));
        }

        if (searchParameters.PetTypes != null && searchParameters.PetTypes.Any())
        {
            announcements = announcements.Where(t => searchParameters.PetTypes.Contains(t.Pet.PetType));
        }

        return Task.FromResult(announcements);
    }

    private Task<Announcement> CreateAsync(Announcement announcement)
    {
        int id = 1;
        if (context.Announcements.Any())
        {
            id = context.Announcements.Max(a => a.Id);
            id++;
        }

        announcement.Id = id;
        
        context.Announcements.Add(announcement);
        context.SaveChanges();

        return Task.FromResult(announcement);
    }

    public Task UpdateAsync(AnnouncementUpdateDto announcement)
    {
        Announcement? existing = context.Announcements.FirstOrDefault(a => a.Id == announcement.Id);
        if (existing == null)
        {
            throw new Exception($"Announcement with id {announcement.Id} does not exist!");
        }

        Announcement createdAnnouncement = new Announcement
        {
            Id = announcement.Id,
            PetOwner = existing.PetOwner,
            CreationDateTime = existing.CreationDateTime,
            EndDate = announcement.EndDate,
            StartDate = announcement.StartDate,
            Pet = announcement.Pet ?? existing.Pet,
            PostalCode = announcement.PostalCode == null ? existing.PostalCode : announcement.PostalCode,
            ServiceDescription = announcement.ServiceDescription == null ? existing.PostalCode : announcement.ServiceDescription
        };
        context.Announcements.Remove(existing);
        context.Announcements.Add(createdAnnouncement);
        
        context.SaveChanges();

        return Task.CompletedTask;
    }

    public Task DeleteAsync(int id)
    {
        Announcement? existing = context.Announcements.FirstOrDefault(announcement =>
            announcement.Id == id);
        if (existing == null)
        {
            throw new Exception($"Announcement with id {id} does not exist!");
        }

        context.Announcements.Remove(existing);
        context.SaveChanges();
        
        return Task.CompletedTask;
    }
}