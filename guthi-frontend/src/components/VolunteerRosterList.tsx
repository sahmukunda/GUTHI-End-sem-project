export type VolunteerRoster = {
  needed: number;
  accepted: string[];
  waiting: string[];
  declined: string[];
};

function NameGroup({ title, names }: { title: string; names: string[] | null | undefined }) {
  const rows = names ?? [];
  return (
    <div className="roster__group">
      <div className="roster__label">{title}</div>
      {rows.length === 0 ? (
        <p className="roster__empty">None</p>
      ) : (
        <ul className="roster__list">
          {rows.map((name, index) => (
            <li key={`${name}-${index}`}>{name}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

export function VolunteerRosterList({ roster }: { roster: VolunteerRoster | null }) {
  if (!roster) return null;
  return (
    <div className="roster">
      <p className="roster__need">Needs {roster.needed}</p>
      <NameGroup title="Accepted" names={roster.accepted} />
      <NameGroup title="Still to answer" names={roster.waiting} />
      <NameGroup title="Declined" names={roster.declined} />
    </div>
  );
}
